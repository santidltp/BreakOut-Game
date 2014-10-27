package BOut;

public class PowerUpStrategyDecoratorBall extends PowerUpStrategyDecorator {
  public PowerUpStrategyDecoratorBall (PowerUpStrategy decorated) {
	    super(decorated);
	  }
	  @Override
	  public void activate (){
	    super.activate();
	    GameState.getGameState().addBalls(1);//add a ball when the powerup hit the paddle
	  }


}
