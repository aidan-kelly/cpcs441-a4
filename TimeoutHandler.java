package assignment4;
import java.util.TimerTask;

public class TimeoutHandler extends TimerTask {
		Router router;
		
		//constructor definition
		public TimeoutHandler(Router router){
			this.router = router;
		}

		public void run() {
			this.router.sendPkts();
		}

}