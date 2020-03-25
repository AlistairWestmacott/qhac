package uk.ac.cam.amw223.qhac;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class markov {

    Map<Integer, Map<queueState, Double>> monthProbabilities;
    Map<Integer, Map<queueState, Double>> weekDayProbabilities;
    Map<Integer, Map<queueState, Double>> hourProbabilities;
    Map<queueState, Map<queueState, Double>> transitionTable;

    ride Ride;

    public markov(ride Ride) {
        this.Ride = Ride;
    }

    public void train(List<Path> data) {
        List<String> lines;
        List<String> datum;
        List<String> fileName;
        int currentHour;
        int currentDay;
        int currentMonth;

        // use normalisation, could be useful if the order of the markov assumption is increased
        initialiseTables(1.0);

        Map<queueState, Double> currentInnerRow;

        queueState previousState = queueState.OPENS_SOON;
        queueState currentState;

        String nextStateToParse;

        for (Path p : data) {
            try {
                lines = Files.readAllLines(p);
                fileName = parseLine(p.getFileName().toString(), '_');
                currentDay = dayToInt(fileName.get(2));
                currentMonth = monthToInt(fileName.get(1));

                for (String line : lines) {
                    datum = parseLine(line, ',');
                    currentHour = timeToInt(datum.get(0));

                    nextStateToParse = datum.get(Ride.ordinal() + 1);

                    if (nextStateToParse.equals("")) {
                        System.out.println("nextStateToParse is empty. Error found in file " + p.toString() + " : ");
                        System.out.println(line);
                    }

                    if (nextStateToParse.toCharArray()[0] == 'O') {
                        currentState = queueState.OPENS_SOON;// first column is time value
//                    } else if (nextStateToParse.equals("Currently Unavailable")) {
//                        currentState = queueState.CURRENTLY_UNAVAILABLE;
                    } else {
                        currentState = queueState.strToState(datum.get(Ride.ordinal() + 1));// first column is time value
                    }


                    // increment transition from previous state to next state
                    currentInnerRow = transitionTable.get(previousState);
                    currentInnerRow.put(currentState, currentInnerRow.get(currentState) + 1);

                    currentInnerRow = weekDayProbabilities.get(currentDay);
                    currentInnerRow.put(currentState, currentInnerRow.get(currentState) + 1);

                    currentInnerRow = hourProbabilities.get(currentHour);
                    currentInnerRow.put(currentState, currentInnerRow.get(currentState) + 1);

                    currentInnerRow = monthProbabilities.get(currentMonth);
                    currentInnerRow.put(currentState, currentInnerRow.get(currentState) + 1);

                    previousState = currentState;
                }
            } catch (IOException e) {
                System.out.println("error with file " + p.toString());
                System.out.println(e.getMessage());
            }
        }

        // Use logs since will be multiplying many probabilities so logs turn that into addition

        normaliseTransitionProbabilities(transitionTable);
        normaliseProbabilities(hourProbabilities);
        normaliseProbabilities(weekDayProbabilities);
        normaliseProbabilities(monthProbabilities);

    }

    public double test(List<Path> testSet, int delta) {
        // delta is how far into the future the program will predict

        List<String> lines;
        List<String> datum;
        List<String> fileName;
        int currentHour;
        int currentDay;
        int currentMonth;

        Map<Path, Map<Integer, queueState>> future = new HashMap<>();
        Map<Path, Map<Integer, queueState>> actual = new HashMap<>();

        Map<Integer, queueState> futuresForFile;
        Map<Integer, queueState> actualForFile;

        int time = 0;

        double correctAnswers = 0;
        double totalAnswers = 0;

        Map<queueState, Double> currentInnerRow;

        queueState previousState = queueState.OPENS_SOON;
        queueState currentState;
        queueState futureState;
        String nextStateToParse;

        for (Path p : testSet) {
            try {
                futuresForFile = new HashMap<>();
                actualForFile = new HashMap<>();

                time = 0;

                lines = Files.readAllLines(p);
                fileName = parseLine(p.getFileName().toString(), '_');
                currentDay = dayToInt(fileName.get(2));
                currentMonth = monthToInt(fileName.get(1));

                for (String line : lines) {
                    datum = parseLine(line, ',');
                    currentHour = timeToInt(datum.get(0));

                    nextStateToParse = datum.get(Ride.ordinal() + 1);

                    if (nextStateToParse.toCharArray()[0] == 'O') {
                        currentState = queueState.OPENS_SOON;// first column is time value
//                    } else if (nextStateToParse.equals("Currently Unavailable")) {
//                        currentState = queueState.CURRENTLY_UNAVAILABLE;
                    } else {
                        currentState = queueState.strToState(datum.get(Ride.ordinal() + 1));// first column is time value
                    }

                    actualForFile.put(time, currentState);
                    futureState = previousState;
                    for (int i = 1; i < delta; i++) {
                        futureState = predictNextMinute(futureState, currentHour, currentDay, currentMonth);
                    }
                    futuresForFile.put(time + delta - 1, futureState);

                    if (futuresForFile.get(time) != null) {
                        totalAnswers++;
                        if (actualForFile.get(time) == futuresForFile.get(time)) {
                            correctAnswers++;
                        }
                    }

                    time++;
                    previousState = currentState;
                }

                future.put(p, futuresForFile);
                actual.put(p, actualForFile);

            } catch (IOException e) {
                System.out.println("error with file " + p.toString());
                System.out.println(e.getMessage());
            }
        }

        return correctAnswers/totalAnswers;
    }

    private double calculateAccuracy() {
        return 0;
    }

    private void initialiseTables(double val) {
        Map<queueState, Double> currentRow;

        // initialise all cells to 0 for normalisation.

        transitionTable = new HashMap<>();
        for (queueState s : queueState.values()) {
            currentRow = new HashMap<>();
            for (queueState t : queueState.values()) {
                currentRow.put(t, val);
            }
            transitionTable.put(s, currentRow);
        }

        hourProbabilities = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            currentRow = new HashMap<>();
            for (queueState s : queueState.values()) {
                currentRow.put(s, val);
            }
            hourProbabilities.put(i, currentRow);
        }

        weekDayProbabilities = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            currentRow = new HashMap<>();
            for (queueState s : queueState.values()) {
                currentRow.put(s, val);
            }
            weekDayProbabilities.put(i, currentRow);
        }

        monthProbabilities = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            currentRow = new HashMap<>();
            for (queueState s : queueState.values()) {
                currentRow.put(s, val);
            }
            monthProbabilities.put(i, currentRow);
        }
    }

    private void normaliseProbabilities(Map<Integer, Map<queueState, Double>> probs) {
        Map<Integer, Double> count = new HashMap<>();
        for (int i = 0; i < probs.size(); i++) {
            count.put(i, 0.0);
        }
        for (Map.Entry<Integer, Map<queueState, Double>> row : probs.entrySet()) {
            for (Map.Entry<queueState, Double> entry : row.getValue().entrySet()) {
                count.put(row.getKey(), count.get(row.getKey()) + entry.getValue());
            }
        }
        for (Map.Entry<Integer, Map<queueState, Double>> row : probs.entrySet()) {
            for (Map.Entry<queueState, Double> entry : row.getValue().entrySet()) {
                entry.setValue(Math.log(entry.getValue() / count.get(row.getKey())));
            }
        }
    }

    private void normaliseTransitionProbabilities(Map<queueState, Map<queueState, Double>> probs) {
        Map<queueState, Double> count = new HashMap<>();
        for (queueState s : queueState.values()) {
            count.put(s, 0.0);
        }
        for (Map.Entry<queueState, Map<queueState, Double>> row : probs.entrySet()) {
            for (Map.Entry<queueState, Double> entry : row.getValue().entrySet()) {
                count.put(row.getKey(), count.get(row.getKey()) + entry.getValue());
            }
        }
        for (Map.Entry<queueState, Map<queueState, Double>> row : probs.entrySet()) {
            for (Map.Entry<queueState, Double> entry : row.getValue().entrySet()) {
                entry.setValue(Math.log(entry.getValue() / count.get(row.getKey())));
            }
        }
    }

    public queueState predictNextMinute(queueState previous, int hour, int day, int month) {
        queueState prediction = null;

        double maxProbability = Double.NEGATIVE_INFINITY;
        double currentProbability;

        for (queueState s : queueState.values()) {
            currentProbability = transitionTable.get(previous).get(s)
                    + hourProbabilities.get(hour).get(s)
                    + weekDayProbabilities.get(day).get(s)
                    + monthProbabilities.get(month).get(s);
            if (currentProbability > maxProbability) {
                maxProbability = currentProbability;
                prediction = s;
            }
        }

        return prediction;
    }

    private int timeToInt(String t) {
        return Integer.parseInt(t.substring(0, 2));// should be two characters
    }

    private List<String> parseLine(String line, char delimiter) {
        List<String> tokens = new ArrayList<>();
        String current = "";
        for (char c : line.toCharArray()) {
            if (c == delimiter) {
                tokens.add(current);
                current = "";
            } else {
                current += c;
            }
        }
        tokens.add(current);
        return tokens;
    }

    private int dayToInt(String day) {
        if (day.equals("Monday")) {
            return 0;
        } else if (day.equals("Tuesday")) {
            return 1;
        } else if (day.equals("Wednesday")) {
            return 2;
        } else if (day.equals("Thursday")) {
            return 3;
        } else if (day.equals("Friday")) {
            return 4;
        } else if (day.equals("Saturday")) {
            return 5;
        } else if (day.equals("Sunday")) {
            return 6;
        } else {
            return -1;
        }
    }

    private int monthToInt(String month) {
        if (month.equals("January")) {
            return 0;
        } else if (month.equals("February")) {
            return 1;
        } else if (month.equals("March")) {
            return 2;
        } else if (month.equals("April")) {
            return 3;
        } else if (month.equals("May")) {
            return 4;
        } else if (month.equals("June")) {
            return 5;
        } else if (month.equals("July")) {
            return 6;
        } else if (month.equals("August")) {
            return 7;
        } else if (month.equals("September")) {
            return 8;
        } else if (month.equals("October")) {
            return 9;
        } else if (month.equals("November")) {
            return 10;
        } else if (month.equals("December")) {
            return 11;
        } else {
            return-1;
        }
    }

    private Map<queueState, Double> getTransRow(double val) {
        Map<queueState, Double> row = new HashMap<>();
        for (queueState s : queueState.values()) {
            row.put(s, val);
        }
        return row;
    }
}
