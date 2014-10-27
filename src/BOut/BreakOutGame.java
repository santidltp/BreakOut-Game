package BOut;

import com.golden.gamedev.*;
import com.golden.gamedev.engine.BaseInput;
import com.golden.gamedev.object.*;
import com.golden.gamedev.object.background.*;
import com.golden.gamedev.object.collision.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import static BOut.BreakOutEngine.*;
import XmlImporter.*;


public class BreakOutGame extends GameObject 
{
//instances
  PlayField playField;
  Background background;
  SpriteGroup balls;
  SpriteGroup miniballs;
  SpriteGroup paddles;
  SpriteGroup blocks;
  SpriteGroup powerUps;
  Paddle gamePaddle;
  static final int PADDLE_WIDTH = 104;
  GameFont scoreFont;
  GameFont doneFont;
  boolean noEndSound = true;
  private static final String WIN_SOUND = SoundsDirectory + "you-win.wav";
  private static final String LOSE_SOUND = SoundsDirectory + "you-lose.wav";
  private Memento snapshot = null;
					  /* *************************************************
					   * The following added instances are for           *
					   * the graphical display; ballsDisplay             *
					   * will display the big balls and                  *
					   * miniballsDisplay to display the miniballs.      *
					   * Two sprite group are required here in order     *
					   * to display them as a group.**********************/
						 BallsDisplay         ballsDisplay;
						 BallsDisplay         miniballsDisplay;
						 SpriteGroup          ballsStack;
						 SpriteGroup          miniballsStack;
  
  public static class Memento implements Serializable
  {

    private final List<Ball> balls = new ArrayList<Ball>();
    private final Paddle thePaddle;
    private final List<Block> blocks = new ArrayList<Block>();
    private final List<PowerUp> powerUps = new ArrayList<PowerUp>();
    private final List<Miniball> miniballs = new ArrayList<Miniball>();
    private final GameState state;
    private static Memento makeMemento (BreakOutGame game, GameState state)   {      return new Memento(game, state);    }
    

    private Memento (BreakOutGame game, GameState state)
    {
      for (Sprite ball : game.balls.getSprites())
      {
        if (ball != null && ball.isActive())
        {
          balls.add(((Ball)ball).memento());
        }
      }
      thePaddle = game.gamePaddle.memento();
      for (Sprite block : game.blocks.getSprites())
      {
        if (block != null && block.isActive())
        {
          blocks.add(((Block)block).memento());
        }
      }
      for (Sprite powerUp : game.powerUps.getSprites())
      {
        if (powerUp != null && powerUp.isActive())
        {
          powerUps.add(((PowerUp)powerUp).memento());
        }
      }
      for (Sprite miniball : game.miniballs.getSprites())
      {
        if (miniball != null && miniball.isActive())
        {
          miniballs.add(((Miniball)miniball).memento());
        }
      }
      this.state = state.memento();
    }
    

    public void restoreStateAndSprites (BreakOutGame g)
    {
      GameState.setGameState(this.state.memento());

      g.gamePaddle = this.thePaddle.memento();
      g.addPaddle(g.gamePaddle);

      for (Ball b : this.balls)
      {
        g.addBall(b.memento());
      }
      for (Miniball b : this.miniballs)
      {
        g.addMiniball(b.memento());
      }
      for (Block b : this.blocks)
      {
        g.addBlock(b.memento());
      }
      for (PowerUp p : powerUps)
      {
        g.addPowerUp(p.memento());
      }
    }
  }
  

  public Memento memento ()
  {
    return Memento.makeMemento(this, GameState.getGameState());
  }
  

  private static String makeArchiveFileName ()
  {
    Calendar now = Calendar.getInstance();
    String filename = String.format("%04d-%02d-%02d-%02d-%02d-%02d.sav",
        now.get(Calendar.YEAR), now.get(Calendar.MONTH)+1, now.get(Calendar.DAY_OF_MONTH),
        now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
    return filename;
  }
  

  private void archiveGame ()
  {
    try {
      Memento snapshot = this.memento();
      String filename = makeArchiveFileName();
      FileOutputStream fout = new FileOutputStream(filename);
      ObjectOutputStream oout = new ObjectOutputStream(fout);
      oout.writeObject(snapshot);
      oout.close();
    }
    catch (FileNotFoundException exc)
    {
      System.out.printf("Could not open output file for saving; giving up!%nInfo:%n%s", exc);
    }
    catch (IOException exc)
    {
      System.out.printf("IO problem saving; giving up!%nInfo:%n%s", exc);
    }
  }

  public static Memento restoreSnapshot (String filename)
  {
    try {
      FileInputStream fin = new FileInputStream(filename);
      ObjectInputStream oin = new ObjectInputStream(fin);
      Memento snapshot = (Memento)oin.readObject();
      oin.close();
      return snapshot;
    }
    catch (IOException exc)
    {
      System.out.printf("IO exception restoring; giving up!%nInfo:%n%s", exc);
    }
    catch (ClassNotFoundException exc)
    {
      System.out.printf("Class not found while restoring; giving up!%nInfo:%n%s", exc);
    }
    return null;
  }

  public BreakOutGame (GameEngine engine)
  {
    super(engine);
  }

  public BreakOutGame (GameEngine engine, Memento snapshot)
  {
    super(engine);
    this.snapshot = snapshot;
  }

  @Override
  public void initResources () 
  {
    initEventSources();
    background = createBackground();
    playField = createPlayField(background);
    SpriteGroup[] groups = createSpriteGroups();
    addSpriteGroupsToPlayField(playField, groups);
    createAndAddCollisionGroups(playField, background);
    initializeFonts();
    setDisplayRate(60);
    if (snapshot == null)
    {
      GameState.getGameState().startLevel();
      createPaddle(background);
      createBlocks();
    }
    else
    {
      // the restore-from-snapshot case
      snapshot.restoreStateAndSprites(this);
      GameState.getGameState().restart();
    }
  }

  private void initEventSources ()
  {
    Ball.newGame();
    Miniball.newGame();
    PowerUp.newGame();
    Block.newGame();
  }


  private Background createBackground ()  {    return new ColorBackground(Color.gray, 800, 600);  }

  private PlayField createPlayField (Background background)
  {
    // create the playing field
    PlayField playField = new PlayField();
    playField.setComparator(new Comparator<Sprite>(){
      public int compare (Sprite s1, Sprite s2)
      {
        return s1.getLayer() - s2.getLayer();
      }
    });
    playField.setBackground(background);
    return playField;
  }
  /* ***************************************************************************
   * Here we have to create the groups of sprites for the balls and display    *
   * them as a group. We have to make sure to show our balls to the professor  *
   * the way he wants to see them. Two columns of big balls, five each column. *
   * Five columns of miniballs, ten each column.							*
   *****************************************************************************/
  private SpriteGroup[] createSpriteGroups ()
  {
    balls     			= new SpriteGroup("balls");
    miniballs 			= new SpriteGroup("miniballs");
    paddles   			= new SpriteGroup("paddles");
    blocks    			= new SpriteGroup("blocks");
    powerUps  			= new SpriteGroup("powerUps");
    ballsDisplay = new BallsDisplay(2, 5, getImageFromFile("Ball"), 0, 550);
    ballsDisplay.showBalls(GameState.getGameState().getBaseBallsRemaining());
    ballsStack = ballsDisplay.getSpriteGroup();
    
    miniballsDisplay = new BallsDisplay(5, 10, getImageFromFile("Miniball"), (2 + getImageFromFile("Ball").getWidth() + 2) * 2, 550);
    miniballsDisplay.showBalls(GameState.getGameState().getBaseMiniBallsRemaining());
    miniballsStack = miniballsDisplay.getSpriteGroup();
    return new SpriteGroup[]{balls, miniballs, paddles, blocks, powerUps, ballsStack, miniballsStack};
  }

  private void addSpriteGroupsToPlayField (PlayField playField, SpriteGroup[] groups)
  {
    for (SpriteGroup group : groups)
    {
      playField.addGroup(group);
    }
  }

  private void createAndAddCollisionGroups (PlayField playField, Background background)
  {
    CollisionBounds boundBallColl = new CollisionBounds(background) {
      public void collided (Sprite ball) {
        Ball theBall = (Ball) ball;
        // tell the Ball of the collision, and which side(s) are involved
        theBall.collisionWithBounds(
            this.isCollisionSide(CollisionBounds.TOP_COLLISION   ),
            this.isCollisionSide(CollisionBounds.BOTTOM_COLLISION),
            this.isCollisionSide(CollisionBounds.LEFT_COLLISION  ),
            this.isCollisionSide(CollisionBounds.RIGHT_COLLISION ));
      }
    };
    
    CollisionBounds boundMiniballColl = new CollisionBounds(background) {
      public void collided (Sprite miniball) {
        Miniball theMiniball = (Miniball) miniball;
        theMiniball.collisionWithBounds();
      }
    };

    CollisionBounds boundPowerUpColl = new CollisionBounds(background) {
      public void collided (Sprite powerUp) {
        PowerUp thePowerUp = (PowerUp) powerUp;
        thePowerUp.collisionWithBounds();
      }
    };
    
    CollisionGroup ballPaddleColl    = new CollisionGroup() {
      public void collided (Sprite ball, Sprite paddle) {
        Paddle thePaddle = (Paddle) paddle;
        Ball theBall = (Ball) ball;
        // tell the Ball of the collision (the Paddle doesn't care)
        theBall.collisionWithPaddle(thePaddle);     
      }
    };
    
    CollisionGroup powerUpPaddleColl = new CollisionGroup() {
      public void collided (Sprite powerUp, Sprite paddle) {
        PowerUp thePowerUp = (PowerUp) powerUp;
        thePowerUp.collisionWithPaddle();
      }
    };
    
    CollisionGroup ballBlockColl     = new CollisionGroup() {
      public void collided (Sprite ball, Sprite block) {
        Block theBlock = (Block) block;
        Ball theBall = (Ball) ball;
        // tell the block it was hit by a Ball
        theBlock.collisionWithBall();
        // compute side of the Block that the Ball hit
        // (determines how Ball bounces)
        boolean onBottom = false;
        boolean onTop    = false;
        boolean onLeft   = false;
        boolean onRight  = false;
        // need to take direction into account to avoid double collisions
        // (because of how Golden T computes collisions)
        if        (((getCollisionSide() & BOTTOM_TOP_COLLISION) != 0) && (theBall.getVerticalSpeed() > 0))
        {
          onBottom = true;
        } else if (((getCollisionSide() & TOP_BOTTOM_COLLISION) != 0) && (theBall.getVerticalSpeed() < 0))
        {
          onTop = true;
        } else if (((getCollisionSide() & LEFT_RIGHT_COLLISION) != 0) && (theBall.getHorizontalSpeed() < 0))
        {
          onLeft = true;
        } else if (((getCollisionSide() & RIGHT_LEFT_COLLISION) != 0) && (theBall.getHorizontalSpeed() > 0))
        {
          onRight = true;
        }
        // tell the Ball about its collision
        theBall.collisionWithBlock(onTop, onBottom, onLeft, onRight);
      }
    };

    CollisionGroup miniballBlockColl = new CollisionGroup() {
      public void collided (Sprite miniball, Sprite block) {
        Block theBlock = (Block)block;
        Miniball theMiniball = (Miniball)miniball;
        // check only one side, and check that it has not been reported before
        if (theBlock.newerMiniball(theMiniball))
        {
          theBlock.collisionWithMiniball(theMiniball);
          theMiniball.collisionWithBlock(theBlock);
        }
      }
    };

    playField.addCollisionGroup(balls    , paddles, ballPaddleColl   );
    playField.addCollisionGroup(powerUps , paddles, powerUpPaddleColl);
    playField.addCollisionGroup(balls    , null   , boundBallColl    );
    playField.addCollisionGroup(miniballs, null   , boundMiniballColl);
    playField.addCollisionGroup(powerUps , null   , boundPowerUpColl );
    playField.addCollisionGroup(balls    , blocks , ballBlockColl    );
    playField.addCollisionGroup(miniballs, blocks , miniballBlockColl);
  }


  private void createPaddle (Background background)
  {
    gamePaddle = new Paddle(this.background.getWidth() / 2,     // centered horizontally
                            this.background.getHeight() - 50);  // near the bottom
    this.addPaddle(gamePaddle);
  }

  private void createBlocks ()
  {
    // set up and load the game:
    // this handles the blocks, etc.
    LevelLoader xmlLoader = new LevelLoader(this);
    xmlLoader.loadGame(((BreakOutEngine)parent).nextLevel);
  }

 
  private void initializeFonts ()
  {
    GameFontManager fontMgr = new GameFontManager();
    scoreFont = fontMgr.getFont(new Font("serif", Font.BOLD, 16), Color.BLACK);
    doneFont  = fontMgr.getFont(new Font("sansserif", Font.BOLD, 16), new Color(128, 0, 0));
  }

  private void setDisplayRate (int fps)  {    this.setFPS(fps);  }

  private String message;


  public void update (long elapsedTime) 
  {
    positionPaddleFromMouse();
    processInput();
    handleEndOfGame();
    message = state.message();
    state.performAction(this, elapsedTime);
  }


  private void positionPaddleFromMouse ()
  {
    gamePaddle.setX(this.getMouseX() - (PADDLE_WIDTH / 2));
  }


  private static enum InputState
  {
    Normal {
      public InputState gotB (BreakOutGame game)
      {
        GameState gs = GameState.getGameState();
        if (gs.canFireBall()) {
          game.startNewBall(gs);
        }
        return Normal;
      }
      public InputState gotM (BreakOutGame game) {
        GameState gs = GameState.getGameState();
        if (gs.canFireMiniball())
        {
          game.fireMiniball();
        }
        return Normal;
      }
      public InputState gotP (BreakOutGame game) { return Pausing; }
      public void performAction (BreakOutGame game, long elapsedTime) {
        GameState gs = GameState.getGameState();
        double msPerFrame = 1000.0D / (double)game.getCurrentFPS();
        gs.decrementWaits((long)(msPerFrame + 0.5D));  // 0.5 to round
        game.playField.update(elapsedTime);
      }
    },
    
    Won
    {
      public InputState gotY (BreakOutGame game) { return menuFinish(game); }
      public String message () { return "Level Over ... CONGRATULATIONS, YOU WON!  Press Y to return to Menu"; }
    },
    
    Lost
    {
      public InputState gotY (BreakOutGame game) { return menuFinish(game); }
      public String message () { return "Level Over ... Sorry, you lost.  Press Y to return to Menu"; }
    },
    
    Archiving
    {
      public InputState gotY (BreakOutGame game)
      {
        game.archiveGame();
        return Normal;
      }
      public String message () { return "Make an archive now? (Y or N)"; }
    },
    
    Pausing
    {
      public InputState gotP (BreakOutGame game) { return Normal; }
      public String message () { return "PAUSED (press P again to unpause)"; }
    },
    
    Quitting
    {
      public InputState gotY (BreakOutGame game) { return menuFinish(game); }
      public String message () { return "QUIT? (Y or N)"; }
    },
    
    Restoring
    {
      public InputState gotY (BreakOutGame game)
      {
        game.parent.nextGame = new BreakOutGame(game.parent, game.snapshot);
        return Finish;
      }
      public String message () { return "Restore from snapshot? (Y or N)"; }
    },
    
    RestoringNoSnapshot
    {
      public String message () { return "No snapshot available; press N to continue"; }
    },
    
    Saving
    {
      public InputState gotY (BreakOutGame game)
      {
        game.snapshot = game.memento();
        return Normal;
      }
      public String message () { return "Make a snaphot now? (Y or N) (any previous one is lost)"; }
    },
    
    Finish
    {
      public void performAction (BreakOutGame game, long elapsedTime) { game.finish(); }
    };
    
    public InputState gotA (BreakOutGame game) { return Archiving; }
    public InputState gotB (BreakOutGame game) { return this; }
    public InputState gotM (BreakOutGame game) { return this; }
    public InputState gotN (BreakOutGame game) { return Normal; }
    public InputState gotP (BreakOutGame game) { return this; }
    public InputState gotQ (BreakOutGame game) { return Quitting; }
    public InputState gotR (BreakOutGame game) { return (game.snapshot == null) ? RestoringNoSnapshot : Restoring; }
    public InputState gotS (BreakOutGame game) { return Saving; }
    public InputState gotY (BreakOutGame game) { return this; } 
    private static InputState menuFinish (BreakOutGame game)
    {
      game.parent.nextGameID = BreakOutEngine.BreakOutGameMenu;
      return Finish;
    }
    public String message() { return null; }
    public void performAction (BreakOutGame game, long elapsedTime) { }
  }
  

  private InputState state = InputState.Normal;
  
  
  private void processInput ()
  {
    switch (getBsInput().getKeyPressed()) {
  
    // "Archive"
    case KeyEvent.VK_A:  state = state.gotA(this);  break;

    // "Ball" (try to fire a ball)
    case KeyEvent.VK_B:  state = state.gotB(this);  break;

    // "Miniball" (try to fire a miniball)
    case KeyEvent.VK_M:  state = state.gotM(this);  break;
    
    // "No"
    case KeyEvent.VK_N:  state = state.gotN(this);  break;

    // "Pause" (and unpause)
    case KeyEvent.VK_P:  state = state.gotP(this);  break;
    
    // "Quit"
    case KeyEvent.VK_Q:  state = state.gotQ(this);  break;
    
    // "Restore" from the snapshot
    case KeyEvent.VK_R:  state = state.gotR(this);  break;
    
    // "Snapshot" (make one)
    case KeyEvent.VK_S:  state = state.gotS(this);  break;
    
    // "Yes"
    case KeyEvent.VK_Y:  state = state.gotY(this);  break;  
    
    // get here on any other key, or none
    default:
      // treat mouse button1 the same as M key
      if (bsInput.isMousePressed(MouseEvent.BUTTON1))
      {
        state = state.gotM(this);
      }
      break;
    }
  }

  /* ***********************************************
   * We want to be able to have a current display  *
   * of balls. In other words, when we fire a ball *
   * we want to have ball less in our ball stack.  *
   *************************************************/
  private void updateDisplay()
  {
	  GameState theState = GameState.getGameState();
		 ballsDisplay.showBalls(theState.getBallsRemaining());
	  miniballsDisplay.showBalls(GameState.getGameState().getMiniballsRemaining());
  }
  

  private void startNewBall (GameState gs)
  {
    Ball ball = new Ball(400, 400);       // start in center of screen
    double speed = gs.getBallSpeed();
    double angle = 0.75D * Math.PI;  // 45 degrees down and to the left
    ball.setVelocityPolar(speed, angle);
    ball.setActive(true);
    this.addBall(ball);
  }


  protected BaseInput getBsInput () {  return bsInput; }


  private void handleEndOfGame ()
  {
    GameState gs = GameState.getGameState();
    if (gs.levelDone())
    {
      if (noEndSound)
      {
        noEndSound = false;
        boolean won = gs.wonLevel();
        this.bsSound.play(won ? WIN_SOUND : LOSE_SOUND);
        if (state == InputState.Normal)
        {
          state = (won ? InputState.Won : InputState.Lost);
        }
      }
      if (getBsInput().isKeyPressed(KeyEvent.VK_ESCAPE))
      {
        parent.nextGameID = BreakOutEngine.BreakOutGameMenu;
        finish();
      }
    }
  }


  private BufferedImage getImageFromFile (String which)
  {
    return getImage(GraphicsDirectory + which + ".png");
  }
 
  public void render (Graphics2D g)
  {
    playField.render(g);
    updateTextualDisplay(g);
    updateDisplay();
  }

 
  private void updateTextualDisplay (Graphics2D g)
  {
    GameState theState = GameState.getGameState();
    scoreFont.drawString(g, "Score: " + theState.getScore(), 10, 10);
    scoreFont.drawString(g, "Multiplier: " + theState.getMultiplier(), 10, 30);
    scoreFont.drawString(g, "Balls left: " + theState.getBallsRemaining(), 10, 50);
    scoreFont.drawString(g, "Miniballs left: " + theState.getMiniballsRemaining(), 10, 70);
    if (message != null)
    {
      doneFont.drawString(g, message, 10, 110);
    }
  }    


  public void addPaddle (Paddle p)
  {
    p.setImage(getImageFromFile("Paddle"));
    p.setBackground(background);
    paddles.add(p);
  }


  public void addBlock (Block b)
  {
    b.refreshImage();
    b.setAudio(bsSound);
    b.setGame(this);
    b.setLayer(-1);
    this.blocks.add(b);
  }


  public void addBall (Ball b)
  {
    b.setImage(getImageFromFile("Ball"));
    b.setBackground(background);
    b.setAudio(bsSound);
    b.setLayer(1);
    balls.add(b);
  }
  

  public void addMiniball (Miniball m)
  {
    m.setImage(getImageFromFile("Miniball"));
    m.setBackground(background);
    m.setAudio(bsSound);
    m.setLayer(1);
    miniballs.add(m);
  }


  public void addPowerUp (PowerUp p)
  {
    p.refreshImage();
    p.setBackground(background);
    p.setAudio(bsSound);
    p.setLayer(1);
    powerUps.add(p);
  }
  

  public void dropPowerUp (double x, double y, String type)
  {
    PowerUp powerUp = PowerUpFactory.createPowerUp(type, this, x, y);
    powerUp.setSpeed(0, .1);
    powerUp.setActive(true);
    this.addPowerUp(powerUp);
  }

  protected void fireBall ()
  {
    GameState gs = GameState.getGameState();
    Ball ball = new Ball(400, 400);       // start in center of screen
    double speed = gs.getBallSpeed();
    double angle = 0.75D * Math.PI;  // 45 degrees down and to the left
    ball.setVelocityPolar(speed, angle);
    ball.setActive(true);
    this.addBall(ball);
  }

  protected void fireMiniball ()
  {
    GameState gs = GameState.getGameState();
    Paddle p = this.gamePaddle;
    double xStart = p.getX() + p.getWidth() / 2;
    double yStart = p.getY() + p.getHeight() / 2;
    Miniball miniball = new Miniball(xStart, yStart);
    miniball.setActive(true);
    miniball.setID(gs.obtainMiniballNumber());
    gs.startMiniballWaitTime(3000);
    this.addMiniball(miniball);
  }
}
