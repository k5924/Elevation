package adp.elevation.ui;

import java.awt.image.BufferedImage;
import java.util.concurrent.RecursiveAction;

import adp.elevation.Configuration;
import adp.elevation.jar.Searcher;

public class RASearcher extends RecursiveAction implements Searcher {

	private final BufferedImage raster;
	private final int startPos;
	private final int endPos;
	private final int length;
	private final SearchUIEnhancement masterListener;
	private final int side = Configuration.side;
	private final double Threshold = Configuration.deviationThreshold;
	private volatile int currentPos = 0;
	private volatile int counter = 0;

	public RASearcher(BufferedImage raster, int startPos, int endPos, int length, SearchUIEnhancement masterListener) {
		// TODO Auto-generated constructor stub
		this.raster = raster;
		this.startPos = startPos;
		this.endPos = endPos;
		this.length = length;
		this.masterListener = masterListener;
	}

	@Override
	protected void compute() {
		// TODO Auto-generated method stub
		if (this.length < this.Threshold) {
			runSearch(new SearchListener() {

				@Override
				public void update(int position, long elapsedTime, long positionsTriedSoFar) {
					// TODO Auto-generated method stub
					return;
				}

				@Override
				public void possibleMatch(int position, long elapsedTime, long positionsTriedSoFar) {
					// TODO Auto-generated method stub
					masterListener.possibleMatch(position, elapsedTime, positionsTriedSoFar);
				}

				@Override
				public void information(String message) {
					// TODO Auto-generated method stub
					return;
				}
			});
			return;
		}

		int split = this.length / 2;
		invokeAll(new RASearcher(this.raster, this.startPos, this.endPos, split, this.masterListener),
				new RASearcher(this.raster, this.startPos + split, this.endPos, this.length - split, this.masterListener));
	}

	// code below here is from Mikes AsbtractSearcher

	@Override
	public int numberOfPositionsToTry() {
		// TODO Auto-generated method stub
		return this.endPos - this.startPos;
	}

	@Override
	public int numberOfPositionsTriedSoFar() {
		// TODO Auto-generated method stub
		return this.counter;
	}

	@Override
	public void runSearch(SearchListener listener) throws SearchCancelledException {
		// TODO Auto-generated method stub
		this.reset();
		listener.information("SEARCHING...");

		final long startTime = System.currentTimeMillis();
		while (true) {
			final int foundMatch = this.findMatch(listener, startTime);
			if (foundMatch >= 0) {
				listener.possibleMatch(foundMatch, System.currentTimeMillis() - startTime,
						numberOfPositionsTriedSoFar());
			} else {
				break;
			}
		}
		listener.information("Finished at " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s\n");
		// listener.information(this.counter + " positions attempted.");
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		this.counter = 0;
		this.currentPos = this.startPos;
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub

	}

	private int findMatch(final SearchListener listener, final long startTime) {
		while (this.counter < numberOfPositionsToTry()) {
			final boolean hit = tryPosition();
			this.currentPos++;
			this.counter++;
			if (hit) {
				return this.currentPos - 1;
			} else if (this.counter % 1000 == 0) {
				listener.update(this.currentPos - 1, System.currentTimeMillis() - startTime,
						numberOfPositionsTriedSoFar());
			}
		}
		return -1;
	}
	
	protected boolean tryPosition() {

		final int x1 = this.currentPos % this.raster.getWidth();
		final int y1 = this.currentPos / this.raster.getWidth();

//		System.out.println( "Position: " + this.currentPosition);
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

				// This code extracts the elevation data from the RGB data
				// according to the following formula, as specified by the
				// source of the example elevation data we are using.
				// height = -10000 + ((R * 256 * 256 + G * 256 + B) * 0.1)

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
//				if ( this.currentPosition == 10) {
//					System.out.print( (x1 + x2) + "," + (y1 + y2) + " == " + x2 + "," + y2 + " ----> ");
//				}
			}
		}

		final double stdev = standardDevPop(heights, count);
		// System.out.println( stdev + " (" + max + ", " + min + ")");
		return stdev < this.Threshold;
	}

	private double standardDevPop(final double[] array, final int size) {
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
