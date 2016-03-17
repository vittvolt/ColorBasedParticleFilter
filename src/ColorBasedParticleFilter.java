import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


public class ColorBasedParticleFilter{
	
	Random r = new Random(1234567893);
	
	List<Particle> particles = new Vector<Particle>();
	int NUMBER_OF_PARTICLES = 200;
	
	Mat initial_frame_hist_b;
	Mat initial_frame_hist_g;
	Mat initial_frame_hist_r;
	Mat current_frame;
	
	int image_width, image_height;
	int tracking_window_width, tracking_window_height;
	
	int mean_x = 0;
	int mean_y = 0;
	
	int mean_x_in_previous_frame = 0;
	int mean_y_in_previous_frame = 0;
	
	//Constructors
	public ColorBasedParticleFilter(int Width, int Height){
		for (int i=0; i < NUMBER_OF_PARTICLES; i++){
			particles.add(new Particle(Width,Height));
		}
	}
	
	public ColorBasedParticleFilter(int X, int Y,int Width, int Height) {
		for (int i=0; i < NUMBER_OF_PARTICLES; i++){
			particles.add(new Particle(X,Y,Width,Height));
		}
	}
	
	//Calculate weights for all particles, re-sample and then move the particles 
	public void on_newFrame(Mat m){
		
		current_frame = m;
		double weights_sum = 0;
		
		Iterator<Particle> i = particles.iterator();
		while (i.hasNext()){
			Particle p = i.next();
			double weight = calc_weight_for_particle(p);
			p.weight = weight;
			
			weights_sum += weight;
		}
		
		//Normalize the weights
		i = particles.iterator();
		while (i.hasNext()){
			Particle p = i.next();
			p.weight = p.weight / weights_sum;
			
			//System.out.println("Normalized Weight: "+p.weight);
		}
		List<Double> weighted_distribution = get_weighted_distribution(particles);
		
		//Re-sample the particles
		List<Particle> new_particles_list = new Vector<Particle>();
		i = particles.iterator();
		while (i.hasNext()){
			i.next();
			Particle resampled_particle = get_new_particle(weighted_distribution);
			new_particles_list.add(resampled_particle);
		}
		
		particles = new_particles_list;
		
		calculate_particles_xy_mean();
		
		move_particle();
		
		Imgproc.rectangle(m, new Point(mean_x, mean_y), new Point(mean_x + tracking_window_width, mean_y + tracking_window_height), new Scalar(255, 0, 0, 255), 3);
		//Debug
		System.out.println("Mean x,y: "+mean_x+" "+mean_y);
		System.out.println("Num of samples: "+particles.size());
		i = particles.iterator();
		while(i.hasNext()){
			Particle p = i.next();
			Imgproc.line(m, new Point(p.x,p.y), new Point(p.x,p.y), new Scalar(0,255,0), 7);
		}
	}
	
	public void set_from_initial_frame(Mat m, int x1, int y1, int x2, int y2){
		
		Vector<Mat> bgr_channels = new Vector<Mat>(3);
				
		//Important!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		Rect rect= new Rect(new Point(x1,y1),new Point(x2,y2));
		Mat submat = m.submat(rect);
		Core.split(submat, bgr_channels); 
		
		initial_frame_hist_b = calculate_histogram(bgr_channels.get(0));
		initial_frame_hist_g = calculate_histogram(bgr_channels.get(1));
		initial_frame_hist_r = calculate_histogram(bgr_channels.get(2));	
	}
	
	public Mat calculate_histogram(Mat m){
		Mat hist = new Mat();
		MatOfFloat range = new MatOfFloat(0f, 256f);
		MatOfInt channel = new MatOfInt(0);
		MatOfInt bins = new MatOfInt(128);
		
		List<Mat> mat_array = new Vector<Mat>();
		mat_array.add(m);
				
		Imgproc.calcHist(mat_array, channel, new Mat(), hist, bins, range, false);
		
		return hist;
	}
	
	public void calculate_particles_xy_mean(){
		mean_x_in_previous_frame = mean_x;
		mean_y_in_previous_frame = mean_y;
		
		mean_x = 0;
		mean_y = 0;
		
		Iterator<Particle> i = particles.iterator();
		while (i.hasNext()){
			Particle p = i.next();
			mean_x += p.x;
			mean_y += p.y;
		}
		mean_x = mean_x / particles.size();
		mean_y = mean_y / particles.size();
	}
	
	public double calc_weight_for_particle(Particle p){
		double weight;
		int x = (int) Math.floor((p.x));
		int y = (int) Math.floor((p.y));
		
		//int x_end = (x + tracking_window_width >= image_width) ? image_width -1 : x + tracking_window_width;
		//int y_end = (y + tracking_window_height >= image_height) ? image_height - 1 : y + tracking_window_height;
		
		int x_end = x + tracking_window_width;
		int y_end = y + tracking_window_height;
		
		if (x_end >= image_width) x_end = image_width - 1;
		if (y_end >= image_height) y_end = image_height -1;
		if (x < 0) {x = 0;}
		if (y < 0) {y = 0;}
		
		if (x_end <= x || y_end <= y){
			return 0;
		}
		
		Rect rect= new Rect(new Point(x,y),new Point(x_end,y_end));
		Mat submat = current_frame.submat(rect);
		Vector<Mat> bgr_channels = new Vector<Mat>(3);
		Core.split(submat, bgr_channels);
		
		Mat hist_b = calculate_histogram(bgr_channels.get(0));
		Mat hist_g = calculate_histogram(bgr_channels.get(1));
		Mat hist_r = calculate_histogram(bgr_channels.get(2));
		
		double correlation_b = Imgproc.compareHist(hist_b, initial_frame_hist_b, 0);
		double correlation_g = Imgproc.compareHist(hist_g, initial_frame_hist_g, 0);
		double correlation_r = Imgproc.compareHist(hist_r, initial_frame_hist_r, 0);
		
		double par = 0.33 * (correlation_b + correlation_g + correlation_r);
		weight = Math.exp(-16 * (1 - par));
		
		//Debubg
		//System.out.println(image_width+" "+image_height+"  "+tracking_window_width+" "+tracking_window_height+" x,end,y,end: "+x+" "+x_end+" "+y+" "+y_end+" Weight: "+ weight);
		
		return weight;
	}
	
	public List<Double> get_weighted_distribution(List<Particle> p){
		List<Double> weighted_distribution = new Vector<Double>();
		
		double accumulation = 0;
		for (int i = 0; i < p.size(); i++){
			Particle t = p.get(i);
			accumulation += t.weight;
			
			weighted_distribution.add(accumulation);
		}
		
		return weighted_distribution;
	}
	
	public Particle get_new_particle(List<Double> weighted_distribution){
		Particle new_particle = new Particle();
		double number = r.nextDouble();
		
		for (int i= 0; i < particles.size(); i++){
			if (i == 0){
				if (number <= weighted_distribution.get(0)){
					new_particle.x = particles.get(i).x;
					new_particle.y = particles.get(i).y;
					break;
				}
			}
			else if (i == particles.size() - 1){
				new_particle.x = particles.get(i).x;
				new_particle.y = particles.get(i).y;
				break;
			}
			else{
				if (number <= weighted_distribution.get(i) && number > weighted_distribution.get(i-1)){
					new_particle.x = particles.get(i).x;
					new_particle.y = particles.get(i).y;
					break;
				}
			}
		}
		
		return new_particle;
	}
	
	public void move_particle(){

		for (int i=0; i<particles.size(); i++){
			Particle particle = particles.get(i);
			double dx = 20 * r.nextDouble() - 10.0;
			double dy = 20 * r.nextDouble() - 10.0;
			
			//Add gaussian noise
			/*double p = Math.exp(- p * p / 0.2) - 0.5;		
			double delta_x = dx * (1 + p);
			
			p = r.nextDouble();
			p = Math.exp(- p * p / 0.2) - 0.5;
			double delta_y = dy * (1 + p); */
			
			//Debug
			//System.out.println("dx,dy: "+dx+" "+dy);
			particle.x = particle.x + dx;
			particle.y = particle.y + dy;
			//System.out.println("After adding dx,dy: "+particle.x+" "+particle.y);
			
			if (particle.x <= 0) particle.x = 0;
			if (particle.x >= image_width) particle.x = image_width-1;
			if (particle.y <= 0) particle.y = 0;
			if (particle.y >= image_height) particle.y = image_height-1;
		}		
	}
	
	public void set_image_size(int w, int h){
		image_width = w;
		image_height = h;
	}
	
	public void set_tracking_window(int w, int h){
		tracking_window_width = w;
		tracking_window_height = h;
	}
}