package adp.elevation.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import adp.elevation.Configuration;
import adp.elevation.jar.Searcher;
import adp.elevation.jar.Searcher.SearchCancelledException;
import adp.elevation.jar.Searcher.SearchListener;

public class SearchUIEnhancement extends JFrame implements SearchListener {
	private static final long serialVersionUID = 1L;

	private final JButton openBigButton = new JButton("Open elevation data");
	private final JLabel mainFilenameLabel = new JLabel();

	private final ImagePanel mainImagePanel = new ImagePanel();

	private final JFileChooser chooser = new JFileChooser();

	private final JLabel outputLabel = new JLabel("information");
	private final JButton startButton = new JButton("Start");
	private final JButton cancelButton = new JButton("Cancel");

	private final JProgressBar progress = new JProgressBar();
	private final JCheckBox isParallel = new JCheckBox("Run in parallel");

	private volatile long startTime;
	private volatile Searcher searcher;
	private volatile Thread running;
	private volatile ForkJoinPool pool = null;
	private volatile BufferedImage raster;

	/**
	 * Construct an SearchUIEnhancement and set it visible.
	 */
	public SearchUIEnhancement() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // kill the application on closing the window

		final JPanel mainFilePanel = new JPanel(new BorderLayout());
		mainFilePanel.add(this.openBigButton, BorderLayout.WEST);
		mainFilePanel.add(this.mainFilenameLabel, BorderLayout.CENTER);
		mainFilePanel.add(this.isParallel, BorderLayout.EAST);
		final JPanel topPanel = new JPanel(new GridLayout(0, 1));
		topPanel.add(mainFilePanel);

		final JPanel imagePanel = new JPanel(new BorderLayout());
		imagePanel.add(mainImagePanel, BorderLayout.CENTER);

		final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(this.startButton);
		buttonPanel.add(this.cancelButton);

		final JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(this.progress, BorderLayout.NORTH);
		bottomPanel.add(this.outputLabel, BorderLayout.CENTER);
		bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

		final JPanel mainPanel = new JPanel(new BorderLayout());

		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(imagePanel, BorderLayout.CENTER);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);

		// w6 tutorial
		this.openBigButton.addActionListener(ev -> {
			if (this.chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				final File file = this.chooser.getSelectedFile();
				this.mainFilenameLabel.setText(file.getName());
				try {
					this.raster = ImageIO.read(file);
				} catch (final IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mainImagePanel.resetHighlights();
				mainImagePanel.setImage(this.raster);
				pack();
				mainImagePanel.repaint();
			}
		});

		this.startButton.addActionListener(ev -> {
			if (this.raster != null) {
				mainImagePanel.resetHighlights();
				mainImagePanel.setImage(this.raster);
				pack();
				mainImagePanel.repaint();
				if (this.isParallel.isSelected()) {
					new Thread(() -> runParallelSearch()).start();
				} else {
					new Thread(() -> runSearch()).start();
				}
			}
		});

		this.cancelButton.addActionListener(ev -> {
			if (this.searcher != null) {
				try {
					this.searcher.cancel();
				} catch (SearchCancelledException SCE) {
					if (pool != null) {
						pool.shutdownNow();
					}
					this.running.interrupt();
				}
			}
		});

		this.chooser.setMultiSelectionEnabled(false);
		this.chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		this.chooser.setCurrentDirectory(new File("rgbelevation"));

		add(mainPanel);
		pack();
		setVisible(true);
	}

	/**
	 * Clears output label and runs the search by calling
	 * {@link Searcher#runSearch(SearchListener)}.
	 */
	private void runSearch() {
		this.running = Thread.currentThread();
		this.searcher = new DevelopedSearcher(this.raster, Configuration.side, Configuration.deviationThreshold);
		new Thread(() -> updateProgress()).start();
		information("information");
		this.progress.setValue(0);
		this.progress.setStringPainted(true);
		this.searcher.runSearch(this);
	}

	private <T> void runParallelSearch() {
		// TODO Auto-generated method stub
		startTime = System.currentTimeMillis();
		this.running = Thread.currentThread();
		this.searcher = new RASearcher(this.raster, 0, (this.raster.getWidth() * this.raster.getHeight()) - 1, this,
				startTime, 0);
		new Thread(() -> updateProgress()).start();
		information("information");
		this.progress.setValue(0);
		this.progress.setStringPainted(true);
		pool = new ForkJoinPool(); // fixes error when trying to allocate jobs after the pool has been shutdown
		try {
			pool.invoke((ForkJoinTask<?>) this.searcher);
		} catch (CancellationException CE) {
			information("Aborted\n");
		}
		pool.shutdown();
		information("Finished at " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s \n");
	}

	private void updateProgress() {
		float currentProgress = this.searcher.numberOfPositionsTriedSoFar();
		float total = this.searcher.numberOfPositionsToTry();

		while ((currentProgress < total) && (!this.running.isInterrupted())) {
			try {
				currentProgress = this.searcher.numberOfPositionsTriedSoFar();
				float percent = (currentProgress / total) * 100;
				this.progress.setValue(Math.round(percent));
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Implements {@link SearchListener#information(String)} by displaying the
	 * information in the UI output label.
	 */
	@Override
	public synchronized void information(final String message) {
		if (!this.running.isInterrupted()) {
			SwingUtilities.invokeLater(() -> this.outputLabel.setText(message + "\n"));
			// this.this.outputLabel.setText(message + "\n");
		} else {
			SwingUtilities.invokeLater(() -> this.outputLabel.setText("Aborted\n" + "\n"));
		}
	}

	/**
	 * Implements {@link SearchListener#possibleMatch(int, long, long)} by
	 * displaying the information in the UI output label.
	 */
	@Override
	public synchronized void possibleMatch(final int position, final long elapsedTime, final long positionsTriedSoFar) {
		final int x = position % this.raster.getWidth();
		final int y = position / this.raster.getWidth();
		information("Possible match at: [" + x + "," + y + "] at " + (elapsedTime / 1000.0) + "s ("
				+ positionsTriedSoFar + " positions attempted)\n");
		if (!this.running.isInterrupted()) {
			final Rectangle r = new Rectangle(x, y, Configuration.side, Configuration.side);
			mainImagePanel.addHighlight(r);
		}
	}

	@Override
	public synchronized void update(final int position, final long elapsedTime, final long positionsTriedSoFar) {
		final int x = position % this.raster.getWidth();
		final int y = position / this.raster.getWidth();
		information("Update at: [" + x + "," + y + "] at " + (elapsedTime / 1000.0) + "s (" + positionsTriedSoFar
				+ " positions attempted)\n");
	}

	private synchronized static void launch() {
		new SearchUIEnhancement();
	}

	private static class ImagePanel extends JPanel {
		private static final long serialVersionUID = 1L;

		private BufferedImage image;

		private final List<Rectangle> highlights = new ArrayList<Rectangle>();

		public void setImage(final BufferedImage image) {
			this.image = image;

			double scale = 1;

			if (image.getWidth() >= image.getHeight()) {
				if (image.getWidth() > 800) {
					scale = 800.0 / image.getWidth();
				}
			} else {
				if (image.getHeight() > 800) {
					scale = 800.0 / image.getHeight();
				}
			}
			final Dimension d = new Dimension((int) Math.ceil(image.getWidth() * scale),
					(int) Math.ceil(image.getHeight() * scale));
			// System.out.println( d);
			setPreferredSize(d);

			invalidate();
			repaint();
		}

		public void addHighlight(final Rectangle r) {
			synchronized (this.highlights) {
				this.highlights.add(r);
			}
			repaint();
		}

		public void resetHighlights() {
			synchronized (this.highlights) {
				this.highlights.clear();
			}
			repaint();
		}

		@Override
		public void paintComponent(Graphics g) {
			if (this.image != null) {
				g = g.create();
				final double scale = getWidth() / (double) this.image.getWidth();
				// System.out.println( scale + "!");
				g.drawImage(this.image, 0, 0, getWidth(), (int) (this.image.getHeight() * scale), this);
				// System.out.println( ">>>" + completed);
				g.setColor(Color.YELLOW);
				synchronized (this.highlights) {
					for (final Rectangle r : this.highlights) {
						final Rectangle s = new Rectangle((int) (r.x * scale), (int) (r.y * scale),
								(int) (r.width * scale), (int) (r.height * scale));
						((Graphics2D) g).draw(s);
						// System.out.println( r + " >> " + s);
					}
				}
			}
		}

	}

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(() -> launch());
	}
}
