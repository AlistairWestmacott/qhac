package uk.ac.cam.amw223.qhac;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class markovTester {

    static final Path dataDirectory = Paths.get("data/2019");

    static private Map<ride, markov> models;

    public static void main(String[] args) throws IOException {

        models = new HashMap<>();

        Map<ride, double[]> cvAccuracies = new HashMap<>();

        double[] cvAccuracy = new double[10];


        // separate data paths for 10 fold cross validation
        List<Path> dataFiles = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dataDirectory)) {
            for (Path item : files) {
                dataFiles.add(item);
            }
        } catch (IOException e) {
            throw new IOException("Cant access the dataset.", e);
        }

        // cross validation

        int seed = 0;

        List<List<Path>> splits = new ArrayList<>();

        //initialise the list with 10 elements
        for (int i = 0; i < 10; i++) {
            splits.add(new ArrayList<>());
        }

        // used to randomise the set of paths (must be list since sets don't have order)
        List<Path> paths = new ArrayList<>(dataFiles);
        Collections.shuffle(paths, new Random(seed));

        // go through elements in path adding to one of the hash maps in splits
        // same process as if dealing cards to a group of people
        int count = 0;

        // go through all entries in paths and add the entry from dataset corresponding to that path to
        // the count'th element in splits. Then increment and modulo 10 count

        for (Path p : paths) {
            splits.get(count).add(p);
            count++;
            count %= 10;
        }
        List<Path> testSplit;
        List<Path> trainingSplit;

        for (ride r : ride.values()) {

            models.put(r, new markov(r));

            cvAccuracies.put(r, new double[10]);

            for (int i = 0; i < 10; i++) {
                trainingSplit = new ArrayList<>();
                for (int j = 0; j < 10; j++) {
                    if (i != j) {
                        trainingSplit.addAll(splits.get(j));
                    }
                }
                testSplit = splits.get(i);

                models.put(r, new markov(r));

                models.get(r).train(trainingSplit);

                // test galactica on testSplit

                cvAccuracy = cvAccuracies.get(r);
                cvAccuracy[i] = models.get(r).test(testSplit);
                cvAccuracies.put(r, cvAccuracy);

            }
            System.out.println(r + " has prediction accuracy of " + findAvg(cvAccuracy));
        }


    }


    private static double findAvg(double[] scores) {
        double result = 0.0;
        for (double element : scores) {
            result += element;
        }
        result /= scores.length;
        return result;
    }

}
