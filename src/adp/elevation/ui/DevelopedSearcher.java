package adp.elevation.ui;

import java.awt.image.BufferedImage;

import adp.elevation.jar.AbstractSearcher;

public class DevelopedSearcher extends AbstractSearcher {

	public DevelopedSearcher(BufferedImage raster, int side, double deviationThreshold) {
		super(raster, side, deviationThreshold);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void cancel() {
		throw new SearchCancelledException();
	}

}
