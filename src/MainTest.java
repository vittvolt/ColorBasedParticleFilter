import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import java.lang.*;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import org.opencv.videoio.VideoCapture;

public class MainTest
{
	public static JFrame frame = new JFrame();
	public static JLabel lbl = new JLabel();
	
	static ColorBasedParticleFilter mFilter = null;
	static Mat img = null;
	static boolean set = false;
	static boolean in_process = true;
	
	static int X1 = 0;
	static int X2 = 0;
	static int Y1 = 0;
	static int Y2 = 0;
	
	public static void show_Img(Image img){
		ImageIcon icon = new ImageIcon(img);
		lbl.setIcon(icon);
  	  	frame.setSize(img.getWidth(null) + 100, img.getHeight(null) + 100);
  	  	frame.add(lbl);
  	  	frame.revalidate();
  	  	frame.repaint();
	}
	
	public static void main (String[] args) throws java.lang.Exception
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		KeyAdapter KL = new KeyAdapter() {
			@Override
	        public void keyPressed(KeyEvent e) {
	            if (e.isShiftDown()){
	            	in_process = false;
	            }
	        }
		};
		
		MouseAdapter MA = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				X1 = e.getX() - 50;
				Y1 = e.getY() - 41;
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				X2 = e.getX() - 50;
				Y2 = e.getY() - 41;
				
				//Debug
				System.out.println(img.cols()+" "+img.rows()+" Rect: "+X1+" "+Y1+" "+X2+" "+Y2);
				mFilter = new ColorBasedParticleFilter(img.cols(),img.rows());
				mFilter.tracking_window_width = X2 - X1;
				mFilter.tracking_window_height = Y2 - Y1;
				mFilter.image_width = img.width();
				mFilter.image_height = img.height();
				mFilter.calculate_particles_xy_mean();
				mFilter.mean_x_in_previous_frame = mFilter.mean_x;
				mFilter.mean_y_in_previous_frame = mFilter.mean_y;
				mFilter.set_from_initial_frame(img, X1, Y1, X2, Y2);
				
				System.out.println("Mean for first time: "+mFilter.mean_x+" "+mFilter.mean_y);
				set = true;
			}
		};
		
		frame.setVisible(true);
		frame.setLayout(new FlowLayout());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addMouseListener(MA);
		frame.addKeyListener(KL);
				
		/*BufferedImage img1 = ImageIO.read(new File("E:/ball.jpg"));
		BufferedImage img2 = ImageIO.read(new File("E:/download.jpg"));
		
		Mat m1 = img_to_mat(img1);
		Mat m2 = img_to_mat(img2);
		
		Vector<Mat> bgr_channels_1 = new Vector<Mat>(3);
		Core.split(m1, bgr_channels_1);
		
		Vector<Mat> bgr_channels_2 = new Vector<Mat>(3);
		Core.split(m2, bgr_channels_2);
		
		Mat hist1 = calculate_histogram(bgr_channels_1.get(0));
		Mat hist2 = calculate_histogram(bgr_channels_2.get(0));
		
		double result1 = Imgproc.compareHist(hist1, hist1, 0);
		double result2 = Imgproc.compareHist(hist1, hist2, 0);
		
		System.out.println(result1 + "  " + result2);
		
		Mat m11 = new Mat();
		Imgproc.pyrDown(m2, m11);
		
		Mat m22 = new Mat();
		Imgproc.pyrDown(m11, m22);   */
		
		//show_Img(Mat_to_BufferedImage(m22));
		
		
		//Test codes
		List<Double> t = new Vector<Double>();
		Random r = new Random();
		System.out.println("List size:  " + t.size() + "  " + r.nextDouble());
		
		VideoCapture vc = new VideoCapture(0);
		img = new Mat();
		
		vc.read(img);
		
		while (in_process){
			vc.read(img);			
			
			if (mFilter != null && set){
				mFilter.on_newFrame(img);
			}
			
			show_Img(Mat_to_BufferedImage(img));
		}
	}
	
	public static Mat calculate_histogram(Mat m){
		Mat hist = new Mat();
		MatOfFloat range = new MatOfFloat(0f, 256f);
		MatOfInt channel = new MatOfInt(0);
		MatOfInt bins = new MatOfInt(128);
		
		List<Mat> mat_array = new Vector<Mat>();
		mat_array.add(m);
				
		Imgproc.calcHist(mat_array, channel, new Mat(), hist, bins, range, false);
		
		return hist;
	}
	
	public static Mat img_to_mat(BufferedImage img){
		Mat m;
		
		byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		m = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
		m.put(0, 0, pixels);
		return m;
	}
	
	public static Image Mat_to_BufferedImage(Mat m){
		
		int type = BufferedImage.TYPE_BYTE_GRAY;
        if ( m.channels() > 1 ) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte [] b = new byte[bufferSize];
        m.get(0,0,b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);  
		
		return image;
	}
}
