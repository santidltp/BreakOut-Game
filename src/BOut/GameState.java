package BOut;

import java.io.Serializable;


public class GameState implements Cloneable, Serializable
{
// instances
  private int score;
  private int scoreMultiplier;
  private static final int baseMultiplier = 1;
  private static final int maxMultiplier = 3;
  private double ballSpeed;
  private static final double baseSpeed = 0.35D;
  private int ballsRemaining;
  private int ballsActive;
  private static int baseBallsRemaining = 3;
  private int miniballsRemaining;
  private int miniballsActive; 
  private static int baseMiniballsRemaining = 20;
  private int miniballsFired = 0;
  private long miniballWaitTimeRemaining; 
  private int blocksRemaining;
  private int powerUpsActive;
  private static GameState GAMESTATE = new GameState();
  public static GameState getGameState ()  {    return GAMESTATE;  }
  //ACCESSORS AND MUTATORS
public int getMultiplier ()  {    return scoreMultiplier;  }
public int getScore ()  {   return score;  }
public boolean canFireBall ()  {    return (ballsRemaining > 0) & (ballsActive == 0);  }
public double getBallSpeed ()  {    return ballSpeed;  }
public void setBallSpeed (double num)  {    ballSpeed = num;  }
public double getBaseSpeed ()  {    return baseSpeed;  }
public int getBallsRemaining ()  {    return ballsRemaining;  }
public int getMiniballsRemaining ()  {    return miniballsRemaining;  }
public void addMiniballs (int number)  {    miniballsRemaining += number;  }
						/* **************************************************************
						 * We need to be able to add balls everytime the user           *
						 * hits the orange powerup with the paddle.                     *
						 * addBalls could have also been named addBall without          *
						 * passing a parameter and increase a ball at a time;           *
						 * public void addBall(){ ballsRemaining++;}.                   *
						 * However, it all depends on design. The reason why I chose    *
						 * this approach for adding balls is because we might add more  *
						 * than one ball in different levels.                           *
						 ****************************************************************/
						public void addBalls(int iBalls){ ballsRemaining+= iBalls;}				
						public int getBaseBallsRemaining(){ return baseBallsRemaining;}
						public int getBaseMiniBallsRemaining(){ return baseMiniballsRemaining;}
  
		
		
  //METHODS
  protected static void setGameState (GameState gs) { GAMESTATE = gs; }  
  private GameState () {  score = 0; }
	

  public void startLevel ()
  {
    ballSpeed = baseSpeed;
    scoreMultiplier = baseMultiplier;
    ballsRemaining = baseBallsRemaining;
    miniballsRemaining = baseMiniballsRemaining;
    ballsActive = 0;
    miniballsActive = 0;
    blocksRemaining = 0;
    ballsActive = 0;
    powerUpsActive = 0;
    connectEventSources();
  }


  public void restart () {  connectEventSources();}
  

  private void connectEventSources ()
  {
    PowerUp.getActiveChangedSource().register(
        new BreakoutListener<ActiveChangedEvent<PowerUp>>()
        {
          public void happened (ActiveChangedEvent<PowerUp> event)
          {
            activeChanged(event.getSprite(), event.getDelta());
          }
        }
    );
    Ball.getActiveChangedSource().register(
        new BreakoutListener<ActiveChangedEvent<Ball>>()
        {
          public void happened (ActiveChangedEvent<Ball> event)
          {
            activeChanged(event.getSprite(), event.getDelta());
          }
        }
    );
    Miniball.getActiveChangedSource().register(
        new BreakoutListener<ActiveChangedEvent<Miniball>>()
        {
          public void happened (ActiveChangedEvent<Miniball> event)
          {
            activeChanged(event.getSprite(), event.getDelta());
          }
        }
    );
    Block.getActiveChangedSource().register(
        new BreakoutListener<ActiveChangedEvent<Block>>()
        {
          public void happened (ActiveChangedEvent<Block> event)
          {
            activeChanged(event.getSprite(), event.getDelta());
          }
        }
    );
  }
  private void incrementScore (int incr)  {   score += incr * scoreMultiplier;  }
  public void incrementMultiplier ()
  {
    if (scoreMultiplier < maxMultiplier)
      scoreMultiplier++;
  }
	

  public void decrementMultiplier ()
  {
    if (scoreMultiplier > 1)
      scoreMultiplier--;
  }
  
  public boolean canFireMiniball ()  {    return (miniballsRemaining > 0) & (miniballWaitTimeRemaining <= 0);  }
  public int obtainMiniballNumber () {    return ++miniballsFired;  }
  public void decrementWaits (long amount)
  {
    if (miniballWaitTimeRemaining > 0) {
      miniballWaitTimeRemaining -= amount;
    }
  }
  
  public void startMiniballWaitTime (long amount)  {    miniballWaitTimeRemaining = amount;  }
  
  public boolean levelDone ()
  {
    // not done until (a) no powerUpsActive, AND
    // (b) no miniballsActive, AND
    // (c) no blocks left OR (no balls left AND no miniballs left)
    return (powerUpsActive + miniballsActive +
            (blocksRemaining * (ballsActive + ballsRemaining + miniballsRemaining))) == 0;
  }
  
  public boolean wonLevel ()  {    return (blocksRemaining == 0);  }
  private void activeChanged (PowerUp powerUp, int delta)  {    powerUpsActive += delta;  }
  private void activeChanged (Ball ball, int delta)
  {
    ballsActive += delta;
    if (delta > 0)
    {
      ballsRemaining -= delta;
    }
  }


  private void activeChanged (Miniball miniball, int delta)
  {
    miniballsActive += delta;
    if (delta > 0)
    {
      miniballsRemaining -= delta;
    }
  }
  private void activeChanged (Block block, int delta)
  {
    blocksRemaining += delta;
    if (delta < 0)
    {
      incrementScore(block.getValue());
    }
  }
  public GameState memento ()
  {
    try
    {
      return (GameState)this.clone();
    }
    catch (CloneNotSupportedException exc)
    {
      System.err.printf("Problem cloning a GameState:%n%s", exc);
      return null;
    }
  }

}
