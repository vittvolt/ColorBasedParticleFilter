import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Particle{
	double x;
	double y;
	double weight;
	int heading;
	static Random random = new Random(123456712); 
	
	//Generate random particles all over the entire image plane
	Particle(int Width, int Height){
		Integer w = Width;
		Integer h = Height;
		this.x = random.nextDouble() * w.intValue();
		this.y = random.nextDouble() * h.intValue();
		this.weight = 1;
		this.heading = ThreadLocalRandom.current().nextInt(0, 361);
	}
	
	Particle(){
		this.x = 0;
		this.y = 0;
		this.weight = 1;
		this.heading = ThreadLocalRandom.current().nextInt(0, 361);
	}
	
	//Generate a random particle at around location (X,Y)
	Particle(int X, int Y, int radius_x, int radius_y){
		this.x = X + ThreadLocalRandom.current().nextInt(0, 2*radius_x + 1) - radius_x;
		this.y = Y + ThreadLocalRandom.current().nextInt(0, 2*radius_y + 1) - radius_y;
		this.weight = 1;
		this.heading = ThreadLocalRandom.current().nextInt(0, 361);
	}
}