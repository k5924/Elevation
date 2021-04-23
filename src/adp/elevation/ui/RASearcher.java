package adp.elevation.ui;

import java.awt.image.BufferedImage;
import java.util.concurrent.RecursiveAction;

import adp.elevation.Configuration;
import adp.elevation.jar.Searcher;

public class RASearcher extends RecursiveAction implements Searcher {
	private static final long serialVersionUID = 1L;
	private final BufferedImage raster;
	private final int startPos;
	private final int endPos;
	private final int side = Configuration.side;
	private final double Threshold = Configuration.deviationThreshold;
	private final SearchUIEnhancement masterListener;
	private final long startTime;
	private volatile Searcher search1;
	private volatile Searcher search2;
	private volatile int currentPos;
	private volatile int counter;

	public RASearcher(BufferedImage raster, int startPos, int endPos, SearchUIEnhancement masterListener,
			long startTime, int counter) {
		// TODO Auto-generated constructor stub
		this.raster = raster;
		this.startPos = startPos;
		this.currentPos = startPos;
		this.endPos = endPos;
		this.masterListener = masterListener;
		this.startTime = startTime;
		this.counter = counter;
	}

	@Override
	protected void compute() {
		// TODO Auto-generated method stub
		if (this.numberOfPositionsToTry() < this.Threshold) {
			runSearch(new SearchListener() {
				@Override
				public synchronized void update(int position, long elapsedTime, long positionsTriedSoFar) {
					// TODO Auto-generated method stub
					return;
				}

				@Override
				public synchronized void possibleMatch(int position, long elapsedTime, long positionsTriedSoFar) {
					// TODO Auto-generated method stub
					masterListener.possibleMatch(position, elapsedTime, positionsTriedSoFar);
				}

				@Override
				public synchronized void information(String message) {
					// TODO Auto-generated method stub
					return;
				}
			});
			return;
		}

		int split = this.numberOfPositionsToTry() / 2;
		this.search1 = new RASearcher(this.raster, this.startPos, this.endPos - split, this.masterListener, this.startTime,
				this.counter);
		this.search2 = new RASearcher(this.raster, this.startPos + split, this.endPos, this.masterListener, this.startTime,
				this.counter);
		invokeAll((RASearcher) this.search1, (RASearcher) this.search2);
	}

	// code below here is from Mikes AsbtractSearcher

	@Override
	public final int numberOfPositionsToTry() {
		// TODO Auto-generated method stub
		if(this.search1 == null && this.search2 == null) {
			return this.endPos - this.startPos;
		}
		return this.search1.numberOfPositionsToTry() + this.search2.numberOfPositionsToTry();
	}

	@Override
	public final int numberOfPositionsTriedSoFar() {
		// TODO Auto-generated method stub
		if(this.search1 == null && this.search2 == null) {
			return this.counter;
		}
		return this.search1.numberOfPositionsTriedSoFar() + this.search2.numberOfPositionsTriedSoFar();
	}

	@Override
	public void runSearch(SearchListener listener) throws SearchCancelledException {
		// TODO Auto-generated method stub
		synchronized (listener) {
			while (true) {
				final int foundMatch = this.findMatch(listener, this.startTime);
				if (foundMatch >= 0) {
					listener.possibleMatch(foundMatch, System.currentTimeMillis() - this.startTime,
							numberOfPositionsTriedSoFar());
				} else {
					break;
				}
			}
		}
	}

	@Override
	public synchronized void reset() {

	}

	@Override
	public synchronized void cancel() {
		// TODO Auto-generated method stub
		throw new SearchCancelledException();
	}

	private synchronized int findMatch(final SearchListener listener, final long startTime) {
		while (numberOfPositionsTriedSoFar() < numberOfPositionsToTry()) {
			final boolean hit = tryPosition();
			this.currentPos++;
			this.counter++;
			if (hit) {
				return this.currentPos - 1;
			}
		}
		return -1;
	}

	protected synchronized boolean tryPosition() {

		final int x1 = this.currentPos % this.raster.getWidth();
		final int y1 = this.currentPos / this.raster.getWidth();

		double max = -Double.MAX_VALUE;
		double min = Double.MAX_VALUE;
		final double[] heights = new double[this.side * this.side];
		int count = 0;
		for (int x2 = 0; x2 < this.side; x2++) {
			if (x1 + x2 >= this.raster.getWidth()) {
				break;
			}
			for (int y2 = 0; y2 < this.side; y2++) {
				if (y1 + y2 >= this.raster.getHeight()) {
					break;
				}

				long elevation = this.raster.getRGB(x1 + x2, y1 + y2);
				elevation = elevation & 0xFFFFFFFFL; // mask off signed upper 32 bits
				double trueElevation = (long) ((elevation * 0.1) - 10000);
				heights[count++] = trueElevation;

				if (trueElevation >= max) {
					max = trueElevation;
				}
				if (trueElevation <= min) {
					min = trueElevation;
				}
			}
		}

		final double stdev = standardDevPop(heights, count);
		return stdev < this.Threshold;
	}

	private synchronized double standardDevPop(final double[] array, final int size) {
		double sum = 0;
		for (int i = 0; i < size; i++) {
			sum += array[i];
		}
		final double mean = sum / size;
		double variance = 0;
		for (int i = 0; i < size; i++) {
			final double dev = array[i] - mean;
			variance += dev * dev;
		}
		variance /= size;
		return Math.sqrt(variance);
	}

}