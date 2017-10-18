package models;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ml.neuralnet.models.Learnable;
import org.bson.types.ObjectId;
import persistence.mongo.WritableObject;
import persistence.mongo.util.CollectionNames;
import persistence.mongo.util.ObjectFinder;
import persistence.mongo.util.ObjectWriter;

public class Movie implements WritableObject, Learnable {

    private ObjectId _id;
    private String mongoId;

    // IMDb fields
    private long budget;
    private String movieId;
    private String language;
    private String title;
    private String overview;
    private float popularity;
    private Date releaseDate;
    private long revenue;
    private float runtime;
    private String status;
    private String tagLine;
    private float averageVote;
    private long voteCount;

    private Movie() {
        // For Jackson only
    }

    public Movie(String title, String movieId) {
        this.title = title;
        this.movieId = movieId;
    }

    public Movie(String title, String movieId, String[] values) throws ParseException {
        this(title, movieId);

        // Set additional fields
        String value;
        for (int i = 0; i < values.length; i++) {
            value = values[i];
            switch (Integer.valueOf(i)) {
                case 0:
                    this.setBudget(Long.parseLong(value));
                    break;
                case 5:
                    this.setLanguage(value);
                    break;
                case 7:
                    this.setOverview(value);
                    break;
                case 8:
                    this.setPopularity(Float.parseFloat(value));
                    break;
                case 11:
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    Date releaseDate;
                    releaseDate = df.parse(value);
                    this.setReleaseDate(releaseDate);
                    break;
                case 12:
                    this.setRevenue(Long.parseLong(value));
                    break;
                case 13:
                    this.setRuntime(Float.parseFloat(value));
                    break;
                case 15:
                    this.setStatus(value.toLowerCase());
                    break;
                case 16:
                    this.setTagLine(value);
                    break;
                case 18:
                    this.setAverageVote(Float.parseFloat(value));
                    break;
                case 19:
                    this.setVoteCount(Long.parseLong(value));
                    break;
            }
        }
    }

    @Override
    public String toString() {
        return this.title + ": " + this.movieId;
    }

    public long getBudget() {
        return budget;
    }

    public void setBudget(long budget) {
        this.budget = budget;
    }

    public String getMovieId() {
        return movieId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public float getPopularity() {
        return popularity;
    }

    public void setPopularity(float popularity) {
        this.popularity = popularity;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public long getRevenue() {
        return revenue;
    }

    public void setRevenue(long revenue) {
        this.revenue = revenue;
    }

    public float getRuntime() {
        return runtime;
    }

    public void setRuntime(float runtime) {
        this.runtime = runtime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTagLine() {
        return tagLine;
    }

    public void setTagLine(String tagLine) {
        this.tagLine = tagLine;
    }

    public float getAverageVote() {
        return averageVote;
    }

    public void setAverageVote(float averageVote) {
        this.averageVote = averageVote;
    }

    public long getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(long voteCount) {
        this.voteCount = voteCount;
    }

    @Override
    public ObjectWriter writer() {
        return new ObjectWriter(CollectionNames.MOVIES, this);
    }

    @Override
    @JsonIgnore
    public ObjectId getObjectId() {
        return this._id;
    }

    public String getMongoId() {
        return this._id.toString();
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }

    public static ObjectFinder<Movie> finder() {
        return new ObjectFinder<Movie>(CollectionNames.MOVIES, Movie.class);
    }

    @Override
    public List<Double> getInputVals() {
        List<Double> inputVals = new LinkedList<>(Arrays.asList(
                Double.valueOf(this.getPopularity()),
                Double.valueOf(this.getAverageVote())
        ));
        return inputVals;
    }
}
