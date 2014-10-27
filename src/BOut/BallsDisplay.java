package BOut;
import java.awt.image.BufferedImage;
import com.golden.gamedev.object.Sprite;
import com.golden.gamedev.object.SpriteGroup;

public class BallsDisplay {


//instances
	private SpriteGroup spriteGroup;
	private Sprite[] sprites;

	//sprite accessor 
		public SpriteGroup getSpriteGroup(){return spriteGroup;	}

	
	public BallsDisplay(int columns, int rows, BufferedImage image, int Posx, int PosY){

		sprites = new Sprite[columns * rows];
		spriteGroup = new SpriteGroup("ballDisplay");
				int width = image.getWidth();
		int height = image.getHeight();
		
		for (int i = 0; i < sprites.length; i++)

		{
			sprites[i] = new Sprite();
			sprites[i].setImage(image);
			sprites[i].setImmutable(true);
			spriteGroup.add(sprites[i]);
		}

		for (int i = 0; i < columns; i++)
			for (int j = 0; j < rows; j++)
				sprites[i * rows + j].setLocation(Posx + 2 + (width + 2 + 2) * i, PosY - 2 - (height + 2 + 2) * j);

	}

	
	public void showBalls(int amount)

	{
		if (amount > sprites.length) return;

		for (int i = 0; i < sprites.length; i++)

			if (i < amount)
				sprites[i].setActive(true);
			else
				sprites[i].setActive(false);

	}

}

