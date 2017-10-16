import com.opencsv.CSVReader;
import models.Movie;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;

public class ReadData {

    @Test
    public void readMovie() {
        Movie movie = Movie.finder().findOne("{title : 'Avatar'}");
        System.out.println(movie.toString());
    }

    @Test
    public void readCSVFile() {
       String csvFile = "/Users/davidsullivan/Downloads/tmdb-5000-movie-dataset/tmdb_5000_movies.csv";

        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        int lineNumber = 0;

        Movie movie;

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] nextLine;

            try {
                while ((nextLine = reader.readNext()) != null) {

                    if (lineNumber > 0) {

                        String movieId = nextLine[3];
                        String title = nextLine[17];

                        try {
                            movie = new Movie(title, movieId, nextLine);
                            System.out.println(movie.toString());
                        }
                        catch (NumberFormatException e2) {
                            continue;
                        } catch (ParseException e) {
                            continue;
                        }
//                        movie.writer().save();
                    }
                    lineNumber++;
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
