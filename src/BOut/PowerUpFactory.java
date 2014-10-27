package BOut;


public class PowerUpFactory {
  

  private PowerUpFactory () { }
  

  private static final PowerUpStrategy plain  = new PowerUpStrategyPlain();
  private static final PowerUpStrategy incr   = new PowerUpStrategyDecoratorIncr(plain);
  private static final PowerUpStrategy decr   = new PowerUpStrategyDecoratorDecr(plain);
  private static final PowerUpStrategy incDec = new PowerUpStrategyDecoratorDecr(incr);
  private static final PowerUpStrategy mini   = new PowerUpStrategyDecoratorMiniballs(plain);
  private static final PowerUpStrategy extraball = new PowerUpStrategyDecoratorBall(plain);////added from our new powerupstrategydecoratorball class
  

  public static PowerUp createPowerUp (String desc, BreakOutGame theGame, double x, double y)
  {
    String color = "Yellow";
    PowerUpStrategy strategy = plain;
    if (desc.equals("incrOnHit"))
    {
      color = "Purple";
      strategy = incr;
    }
    else if (desc.equals("decrOnMiss"))
    {
      color = "Cyan";
      strategy = decr;
    }
    else if (desc.equals("incrDecr"))
    {
      color = "Blue";
      strategy = incDec;
    }
    else if (desc.equals("miniballs"))
    {
      color = "Red";
      strategy = mini;
    }
   ////create a new powerup orange for extraball
    else if (desc.equals("extra")){
    	color = "Orange";
    	strategy = extraball;
    }
    PowerUpDesc pDesc = PowerUpDesc.getPowerUpDesc(color);
    return new PowerUp(pDesc, x, y, strategy);
  }

}
