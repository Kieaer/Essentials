package essentials.core;

import arc.struct.Array;
import mindustry.entities.traits.BuilderTrait;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class map {
    Graphics2D currentGraphics;
    BufferedImage currentImage;

    public void main() {
        int w = world.width() * tilesize, h = world.height() * tilesize;
        BufferedImage image = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        currentGraphics = image.createGraphics();
        currentImage = image;

        //currentGraphics.draw
        try {
            ImageIO.write(image, "png", new File("out.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
