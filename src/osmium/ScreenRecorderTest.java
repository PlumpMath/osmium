package osmium;

import org.monte.screenrecorder.ScreenRecorder;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.monte.media.math.Rational;
import org.monte.media.Format;
import static org.monte.media.AudioFormatKeys.*;
import static org.monte.media.VideoFormatKeys.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ScreenRecorderTest {

    private static ScreenRecorder screenRecorder;

    public ScreenRecorderTest() throws IOException, AWTException {

        //Create a instance of GraphicsConfiguration to get the Graphics configuration
        //of the Screen. This is needed for ScreenRecorder class.
        GraphicsConfiguration gc = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();

        //Create a instance of ScreenRecorder with the required configurations
        screenRecorder = new ScreenRecorder(gc,
                new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI),
                new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                        CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                        DepthKey, (int)24, FrameRateKey, Rational.valueOf(15),
                        QualityKey, 1.0f,
                        KeyFrameIntervalKey, (int) (15 * 60)),
                new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey,"black",
                        FrameRateKey, Rational.valueOf(30)),
                null);
    }

    public void start() throws IOException {
        screenRecorder.start();
    }

    public List<File> stop() throws IOException {
        screenRecorder.stop();
        List<File> createdMovieFiles = screenRecorder.getCreatedMovieFiles();
        for(File movie : createdMovieFiles){
            System.out.println("New movie created: " + movie.getAbsolutePath());
        }
        return createdMovieFiles;
    }

}
