package adp.elevation.ui;

import java.awt.image.BufferedImage;

import adp.elevation.jar.AbstractSearcher;

public class DevelopedSearcher extends AbstractSearcher {

	private SearchUIEnhancement listener;
	
	public DevelopedSearcher(BufferedImage raster, int side, double deviationThreshold, SearchUIEnhancement listener) {
		super(raster, side, deviationThreshold);
		// TODO Auto-generated constructor stub
		this.listener = listener;
	}

	@Override
	public void cancel() {
		this.listener.cancel();
	}

}
