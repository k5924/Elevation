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
	private final Counter counter = new SynchronizedCounter();
	private final long startTime;
	private volatile int currentPos;

	public RASearcher(BufferedImage raster, int startPos, int endPos, SearchUIEnhancement masterListener, long startTime) {
		// TODO Auto-generated constructor stub
		this.raster = raster;
		this.startPos = startPos;
		this.currentPos = startPos;
		this.endPos = endPos;
		this.masterListener = masterListener;
		this.startTime = startTime;
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
		invokeAll(new RASearcher(this.raster, this.startPos, this.endPos - split, this.masterListener, this.startTime),
				new RASearcher(this.raster, this.startPos + split, this.endPos, this.masterListener, this.startTime));
	}

	// code below here is from Mikes AsbtractSearcher

	@Override
	public final int numberOfPositionsToTry() {
		// TODO Auto-generated method stub
		return this.endPos - this.startPos;
	}

	@Override
	public final int numberOfPositionsTriedSoFar() {
		// TODO Auto-generated method stub
		return this.counter.value();
	}

	@Override
	public void runSearch(SearchListener listener) throws SearchCancelledException {
		// TODO Auto-generated method stub
		synchronized (listener) {
			this.reset();

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
		// TODO Auto-generated method stub
		this.currentPos = this.startPos;
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		throw new SearchCancelledException();
	}

	private synchronized void incrementPos() {
		this.currentPos++;
	}

	private synchronized int decrementPos() {
		return this.currentPos--;
	}

	private synchronized int findMatch(final SearchListener listener, final long startTime) {
		while (numberOfPositionsTriedSoFar() < numberOfPositionsToTry()) {
			final boolean hit = tryPosition();
			incrementPos();
			this.counter.increment();
			if (hit) {
				return decrementPos();
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