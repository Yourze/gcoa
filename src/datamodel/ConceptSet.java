package datamodel;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import common.SimpleTools;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

/**
 * @author wzy
 */
public class ConceptSet {

    static int[][] recommendations;
    double[] similarityOfEachConcept;
    double[] similarityOfEachUser;
    int tempActualConcepts = 0;
    int c = 943;
    int numNewConcept;
    static int num;

    /**
     * The minimal number of relevant concepts for each users.
     * Attention: the name is inappropriate.
     */
    public static final int MIN_CONCEPTS_PER_USER = 0;

    /**
     * The minimal value of similarity of two users;
     */
    public static final double MIN_SIMILARITY_OF_TWO_USER = 100;

    /**
     * The rating matrix object.
     */
    RatingMatrix ratingMatrix;

    /**
     * The dataProcessing object.
     */
    DataProcessing dataProcessing;

    /**
     * The number of users.
     */
    int numUsers;

    /**
     * The number of items.
     */
    int numItems;

    /**
     * An array storing all concepts.
     */
    Concept[] userOrientedConcepts;

    /**
     * User-concept inclusion relationships.
     * -1 stands for no more relevant concepts for this user.
     */
    int[][] userConceptInclusionMatrix;

    /**
     * How many concepts are relevant to this user.
     */
    int[] userConceptCounts;
    int[] tempConceptCounts;
    /**
     * User Sorted(Ascendant)
     */
    int[] ascendantFever;

    /**
     * User Sorted(Decendant)
     */
    int[] decendantFever;

    /**
     * Represent
     */
    int[] represent;

    /**
     * Used to collect information about concept. Each concept recommend time and
     * correct time.
     */
    int[][] informationConcept;

    /**
     * Record the data from jester data excel file.
     */
    static double[][] information;

    /**
     * Record the ratings after standarlize.
     */
    double[][] informationAfterStan;

    /**
     * record which user generate new concept while cross and mutate.
     */
    static boolean[] recordUsers;

    /**
     * Only support relevant number of concepts for one user with this size.
     */
    public static final int USER_MAXIMAL_RELEVANT_CONCEPTS = 1000;

    /**
     * The first constructor
     *
     * @param paraMatrix
     */
    public ConceptSet(RatingMatrix paraMatrix) {
        ratingMatrix = paraMatrix;

        numUsers = ratingMatrix.numUsers();
        numItems = ratingMatrix.numItems();

        userOrientedConcepts = new Concept[1500];
        similarityOfEachConcept = new double[numUsers];
        similarityOfEachUser = new double[numUsers];
        userConceptInclusionMatrix = new int[numUsers][USER_MAXIMAL_RELEVANT_CONCEPTS];
        userConceptCounts = new int[numUsers];
        tempConceptCounts = new int[numUsers];
        SimpleTools.matrixFill(userConceptInclusionMatrix, -1);
        information = new double[24983][100];
        informationAfterStan = new double[24983][100];
        dataProcessing = new DataProcessing();
        recommendations = new int[numUsers][numItems];
        recordUsers = new boolean[numUsers];
        numNewConcept = numUsers;
    }// Of the first constructor

    /**
     * Compute S2.
     *
     * @param @return
     * @return
     */
    public double[][] standarlize() {
        double s2;
        double s;
        double sum = 0;
        double ave = 0;
        double up = 0;
        double count = 0;
        double[][] tempInformationAfterStan = new double[24983][100];
        //Step 1.compute average
        for (int i = 0; i < information.length; i++) {
            for (int j = 0; j < information[0].length; j++) {
                sum += information[i][j];
                count++;
            }//of for j
        }//of for i
        ave = sum / count;
        //Step 2.compute up
        for (int i = 0; i < information.length; i++) {
            for (int j = 0; j < information[0].length; j++) {
                up += (information[i][j] - ave) * (information[i][j] - ave);
            }//of for j
        }//of for i
        //Step 3.compute s2
        s = Math.sqrt(up / count);
        //standarlize
        for (int i = 0; i < information.length; i++) {
            for (int j = 0; j < information[0].length; j++) {
                tempInformationAfterStan[i][j] = (information[i][j] - ave) / s;
            }//of for j
        }//of for i
        return tempInformationAfterStan;
    }//of computeS2

    /**
     * Sort the user according to the number of items they have rated in descendant order.
     *
     * @return
     */
    public int[] computeDecendantFever() {
        // Step 1. Count the number of items for each user.
        int[] tempFever = ratingMatrix.getUserFever();

        // Step 2. Select the current least enthusiast.
        boolean[] tempSelected = new boolean[numUsers];
        int[] resultArray = new int[numUsers];

        int tempMaximal;
        int tempUserIndex = -1;
        for (int i = 0; i < numUsers; i++) {
            tempMaximal = 0;
            for (int j = 0; j < numUsers; j++) {
                if (tempSelected[j]) {
                    continue;
                } // Of if

                if (tempFever[j] >= tempMaximal) {
                    tempMaximal = tempFever[j];
                    tempUserIndex = j;
                } // Of if
            } // Of for j

            resultArray[i] = tempUserIndex;
            tempSelected[tempUserIndex] = true;
        } // Of for i
        return resultArray;
    }// Of computeDecendantFever

    /**
     * Find user rated no film.
     *
     * @return
     */
    public boolean[] findUserHaveNoFilm() {
        boolean[] recordUser = new boolean[numUsers];
        Arrays.fill(recordUser, false);
        for (int i = 0; i < ratingMatrix.formalContext.length; i++) {
            int count = 0;
            for (int j = 0; j < ratingMatrix.formalContext[0].length; j++) {
                if (ratingMatrix.formalContext[i][j]) {
                    count++;
                }
            }
            if (count != 0) {
                recordUser[i] = true;
            }
        }
        return recordUser;
    }

    /**
     * Generate all concepts of formal context.
     *
     * @param paraUsersThreshold
     * @param paraItemsThreshold
     * @return Number of concepts.
     */
    public int generateConcept(int paraUsersThreshold, int paraItemsThreshold) {
        Concept resultConcept = null;
        int count = 0;
        for (int i = 0; i < ratingMatrix.formalContext.length; i++) {
            Concept[] tempNewConcept = ratingMatrix.computeRepresentativeAllOrientedConcept(i, paraItemsThreshold);
            for (int j = 0; j < tempNewConcept.length; j++) {
                count++;
                System.out.println(Arrays.toString(tempNewConcept[j].users) + "," + Arrays.toString(tempNewConcept[j].items));
            }
        }//of for i
        System.out.println(count);
        return count;
    }//of generateConcept

    /**
     * Generate the concept using the user fever in descending order.
     *
     * @param paraUsersThreshold
     * @param paraItemsThreshold
     * @return Actual number of concepts.
     */
    public int generateDeConceptSet(int paraUsersThreshold, int paraItemsThreshold) {
        boolean[] recordUser = new boolean[numUsers];
        recordUser = findUserHaveNoFilm();
        // Step 1. Sort the users according to their fever.
        decendantFever = computeDecendantFever();
        // System.out.println("decent: " + Arrays.toString(decendantFever));
        // Step 2. Generate concepts according to users.
        for (int i = 0; i < decendantFever.length; i++) {
            if (recordUser[decendantFever[i]]) {

                // Step 2.1 Is this user covered?
                if (userConceptCounts[decendantFever[i]] > MIN_CONCEPTS_PER_USER) {
                    continue;
                } // Of if
                // Step 2.2 Construct a concept using this user.
//				System.out.println("for user " + decendantFever[i] + ":");
                Concept tempNewConcept = ratingMatrix.computeRepresentativeOrientedConcept(decendantFever[i],
                        paraItemsThreshold);
                // Step 2.3 Record the concept.
                userOrientedConcepts[decendantFever[i]] = tempNewConcept;
//                System.out.println(Arrays.toString(tempNewConcept.users) + Arrays.toString(tempNewConcept.items));
                // Step 2.4 Update the user-concept relationships.
                int[] tempUsers = tempNewConcept.getUsers();
                for (int j = 0; j < tempUsers.length; j++) {
                    userConceptInclusionMatrix[tempUsers[j]][userConceptCounts[tempUsers[j]]] = decendantFever[i];
                    userConceptCounts[tempUsers[j]]++;
                } // Of for j
            }
        } // Of for i

        for (int i = 0; i < numUsers; i++) {
            if (userOrientedConcepts[i] != null) {
                tempActualConcepts++;
            } // Of if
        } // Of for i
        System.out.println("There are " + tempActualConcepts + " concepts.");
        for (int i = 0; i < userConceptCounts.length; i++) {
            tempConceptCounts[i] = userConceptCounts[i];
        }
        return tempActualConcepts;

    }// Of generateDeConceptSet

    /**
     * replace a user with a more similar user
     *
     * @param paraUser
     * @param paraConcept
     * @param paraInsteadUser
     * @param paraInsteadUserIndex
     * @return Result concepts.
     */
    public Concept replace(int paraUser, Concept paraConcept, int paraInsteadUser, int paraInsteadUserIndex) {
        Concept resultConcept = null;
        for (int i = 0; i < paraConcept.users.length; i++) {
            resultConcept.users[i] = paraConcept.users[i];
        } // of for i
        // replace
        // step 1.compute similarity with each user
        double[] sim = ratingMatrix.computeSimilarityOfEachUser(paraUser);
        boolean[] temp = new boolean[numUsers];
        for (int i = 0; i < paraConcept.users.length; i++) {
            temp[paraConcept.users[i]] = true;
        } // of for i
        // step 2.find the most similar user to replace the user
        double maxSim = -1;
        int maxSimUserIndex = -1;
        for (int i = 0; i < temp.length; i++) {
            if (sim[i] > maxSim && !temp[i]) {
                maxSimUserIndex = i;
                maxSim = sim[i];
            } // of if
        } // of for i
        // step 3.replace
        paraConcept.users[paraInsteadUserIndex] = maxSimUserIndex;
        return resultConcept;
    }// of for replace

    /**
     * Exchange the two concept at the cross site.
     *
     * @param paraConcept1
     * @param paraConcept2
     * @param paraCrossSite
     * @param paraUser
     * @return New Concept after exchange.
     */
    public Concept[] exchange(Concept paraConcept1, Concept paraConcept2, int paraCrossSite, int paraUser) {
        Concept[] result = new Concept[2];
        double sim1 = ratingMatrix.computeSimilarityOfConcepts(paraConcept1);
        double sim2 = ratingMatrix.computeSimilarityOfConcepts(paraConcept2);

        int[] extension1 = new int[paraConcept2.users.length];
        int[] extension2 = new int[paraConcept1.users.length];

        for (int i = 0; i <= paraCrossSite; i++) {
            extension1[i] = paraConcept1.users[i];
            extension2[i] = paraConcept2.users[i];
        } // of for i
        for (int i = paraCrossSite + 1; i < paraConcept2.users.length; i++) {
            extension1[i] = paraConcept2.users[i];
        } // of for i
        for (int i = paraCrossSite + 1; i < paraConcept1.users.length; i++) {
            extension2[i] = paraConcept1.users[i];
        } // of for i
        int a = isHave(extension1, paraUser);
        if (a == 0) {
            // the new extension not contain the object user,it is a invalid extension
            result[0] = null;
        } else if (a == 1) {
            // the new extension contain the object user,generate a new concept and compute similarity value
            int[] intension1 = ratingMatrix.getSuperItems(extension1);
            if (intension1.length > 1) {
                Concept tempConcept1 = new Concept(extension1, intension1);
                double sim = ratingMatrix.computeSimilarityOfConcepts(tempConcept1);
                if (sim >= sim1 || sim >= sim2) {
                    result[0] = tempConcept1;
                } // of if
            } // of if
        } else {
            // the new extension contain more than one object user
            extension1 = isSame(extension1, paraUser);
            // generate new concept
            int[] intension1 = ratingMatrix.getSuperItems(extension1);
            if (intension1.length > 1) {
                Concept tempConcept1 = new Concept(extension1, intension1);
                double sim = ratingMatrix.computeSimilarityOfConcepts(tempConcept1);
                if (sim >= sim1 || sim >= sim2) {
                    result[0] = tempConcept1;
                } // of if
            } // of if
        } // of if
        int b = isHave(extension2, paraUser);
        if (b == 0) {
            // the new extension not contain the object user,it is a invalid extension
            result[1] = null;
        } else if (b == 1) {
            // the new extension contain the object user,generate a new concept and compute
            // similarity value
            int[] intension2 = ratingMatrix.getSuperItems(extension2);
            if (intension2.length > 1) {
                Concept tempConcept2 = new Concept(extension2, intension2);
                double sim = ratingMatrix.computeSimilarityOfConcepts(tempConcept2);
                if (sim >= sim1 || sim >= sim2) {
                    result[1] = tempConcept2;
                } // of if
            } // of if
        } else {
            // the new extension contain more than one object user
            extension2 = isSame(extension2, paraUser);
            // generate new concept
            int[] intension2 = ratingMatrix.getSuperItems(extension2);
            if (intension2.length > 1) {
                Concept tempConcept2 = new Concept(extension2, intension2);
                double sim = ratingMatrix.computeSimilarityOfConcepts(tempConcept2);

                if (sim >= sim1 || sim >= sim2) {
                    result[1] = tempConcept2;
                } // of if
            } // of if
        } // of if
        return result;
    }// of exchange

    /**
     * Is the given user set include the given user?
     *
     * @param paraUserSet
     * @param paraUser
     * @return The result.0 menas not have,1 means have one,2 means the given user is duplicate.
     */
    public int isHave(int[] paraUserSet, int paraUser) {
        int count = 0;
        for (int i = 0; i < paraUserSet.length; i++) {
            if (paraUserSet[i] == paraUser) {
                count++;
            } // of for i
        } // of for i
        if (count == 0) {
            return 0;
        } else if (count == 1) {
            return 1;
        } else {
            return 2;
        } // of if
    }// of isHave

    /**
     * Whether there are duplicate elements in the given user set?
     *
     * @param paraUserSet
     * @param paraUser
     * @return
     */
    public int[] isSame(int[] paraUserSet, int paraUser) {
        List list = new ArrayList();
        for (int i = 0; i < paraUserSet.length; i++) {
            if (!list.contains(paraUserSet[i])) {
                list.add(paraUserSet[i]);
            } // of if
        } // of for i
        int[] resultUserSet = new int[list.size()];
        for (int i = 0; i < resultUserSet.length; i++) {
            resultUserSet[i] = (int) list.get(i);
        } // of for i
        return resultUserSet;
    }// of isSame

    /**
     * Is the given two concepts same?
     *
     * @param paraConcept1
     * @param paraConcept2
     * @return
     */
    public static boolean isConceptSame(Concept paraConcept1, Concept paraConcept2) {
        if (paraConcept1.items.length != paraConcept2.items.length
                || paraConcept1.users.length != paraConcept2.users.length) {
            return false;
        } // Of if
        for (int i = 0; i < paraConcept1.items.length; i++) {
            if (paraConcept1.items[i] != paraConcept2.items[i]) {
                return false;
            } // Of if
        } // Of for i
        return true;
    }// Of isConceptSame

    /**
     * Find the user get new concepts,and record the new concept.
     *
     * @param paraUser
     * @param paraLowUserThreshold
     * @param paraItemThreshold
     */
    public void GCGA(int paraUser, int paraLowUserThreshold, int paraItemThreshold) {
        // Cross
        Concept[] newConceptOfCross = new Concept[200];
        Concept[] newConcept = new Concept[200];
        int count1 = 0;
        for (int i = 0; i < userConceptCounts[paraUser] - 1; i++) {
            for (int j = i + 1; j < userConceptCounts[paraUser]; j++) {
                // generate new concept by cross
                Concept tempNewConcept = ratingMatrix.getNewConceptByCrossUsers(
                        userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]],
                        userOrientedConcepts[userConceptInclusionMatrix[paraUser][j]]);
                // is the new concept effective?
                if ((tempNewConcept.users.length > 1) && (tempNewConcept.items.length > 0)) {
                    // is the new concept better than the old concept?
                    if (ratingMatrix.computeSimilarityOfConcepts(tempNewConcept) > ratingMatrix
                            .computeSimilarityOfConcepts(userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]])
                            || ratingMatrix.computeSimilarityOfConcepts(tempNewConcept) > ratingMatrix
                            .computeSimilarityOfConcepts(
                                    userOrientedConcepts[userConceptInclusionMatrix[paraUser][j]])) {
                        // is the concept same as before
                        // true means same,false means not same
                        if (count1 == 0) {
                            // record the concept
                            newConceptOfCross[count1] = tempNewConcept;
                            System.out.println("After cross(0): " + Arrays.toString(newConceptOfCross[count1].users)
                                    + Arrays.toString(newConceptOfCross[count1].items));
                            count1++;
                        } else {
                            boolean isSame;
                            for (int k = 0; k < count1; k++) {
                                isSame = isConceptSame(tempNewConcept, newConceptOfCross[k]);
                                if (isSame) {
                                    break;
                                } else {
                                    if (k == count1 - 1) {
                                        // record the new concept
                                        newConceptOfCross[count1] = tempNewConcept;
                                        System.out.println("After cross: " + Arrays.toString(newConceptOfCross[count1].users)
                                                + Arrays.toString(newConceptOfCross[count1].items));
                                        count1++;
                                    } // of if
                                } // of if
                            } // of for k
                        } // of if
                    } // of if
                } // of if
            } // of for j
        } // of for i

        // mutate
        int count2 = 0;
        for (int i = 0; i < count1; i++) {
            if (newConceptOfCross[i].users.length <= paraLowUserThreshold) {

                // generate a concept by mutate
                Concept tempNewConcept = ratingMatrix.mutateOrientedConcepts(paraUser, paraItemThreshold,
                        newConceptOfCross[i]);
                if (count2 == 0) {
                    // record the concept
                    newConcept[count2] = tempNewConcept;

                    System.out.println("After mutate(0): " + Arrays.toString(newConcept[count2].users)
                            + Arrays.toString(newConcept[count2].items));
                    count2++;
                } else {
                    // is the concept same
                    // true means same,false means not same
                    boolean isSame;
                    for (int k = 0; k < count2; k++) {
                        isSame = isConceptSame(tempNewConcept, newConcept[k]);
                        if (isSame) {
                            break;
                        } else {
                            if (k == count2 - 1) {
                                // record the new concept
                                newConcept[count2] = tempNewConcept;
                                System.out.println("After mutate: " + Arrays.toString(newConcept[count2].users)
                                        + Arrays.toString(newConcept[count2].items));
                                count2++;
                            } // of if
                        } // of if
                    } // of for k
                } // of if
            } else {
                newConcept[count2] = newConceptOfCross[i];
                count2++;
            } // of if
        } // of for i

        // final
//		if (count2 != 0) {
//			recordUsers[paraUser] = true;
//		} // of if
        // use old concept to recommend
//		Concept[] userConcepts = new Concept[userConceptCounts[paraUser]];
//		for (int i = 0; i < userConceptCounts[paraUser]; i++) {
//			userConcepts[i] = userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]];
//		} // of for i
//		int[] tempRecommendation;
//		tempRecommendation = recommendationTest(paraUser, 0.5, userConcepts);
//		for (int j = 0; j < tempRecommendation.length; j++) {
//			recommendations[paraUser][j] = tempRecommendation[j];
//		} // of for j
        // use new concept to recommend
        if (count2 != 0) {
            Concept[] finalConcept = new Concept[count2];
            for (int i = 0; i < finalConcept.length; i++) {
                finalConcept[i] = newConcept[i];
            } // of for i

//			int[] tempRecommendations;
//			tempRecommendations = recommendationTest(paraUser, 0.5, finalConcept);
//			for (int i = 0; i < tempRecommendations.length; i++) {
//				recommendations[paraUser][i] = tempRecommendations[i];
//			} // of for i

            int[][] tempRecommendations;
            tempRecommendations = recommendationToAllUsers(0.5, finalConcept);
            for (int i = 0; i < tempRecommendations.length; i++) {
                for (int j = 0; j < tempRecommendations[0].length; j++) {
                    recommendations[i][j] = tempRecommendations[i][j];
                } // of for j
            } // of for i
        } // of if
    }// of GCGAv

    /**
     * ACGA
     *
     * @param paraUser
     * @param paraLowUserThreshold
     * @param paraItemThreshold
     */
    public void GCGAv6(int paraUser, int paraLowUserThreshold, int paraItemThreshold) {
        // Cross
        Concept[] newConceptOfCross = new Concept[10000];
        Concept[] newConcept = new Concept[10000];
        int count1 = 0;
        for (int i = 0; i < userConceptCounts[paraUser] - 1; i++) {
            for (int j = i + 1; j < userConceptCounts[paraUser]; j++) {
                if (ratingMatrix.computeSimilarityOfConcepts(userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]]) > ratingMatrix.computeSimilarityOfConcepts(userOrientedConcepts[userConceptInclusionMatrix[paraUser][j]])) {
                    if (count1 == 0) {
                        newConceptOfCross[count1] = userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]];
                        count1++;
                    } else {
                        boolean isSame;
                        for (int k = 0; k < count1; k++) {
                            isSame = isConceptSame(userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]], newConceptOfCross[k]);
                            if (isSame) {
                                break;
                            } else {
                                if (k == count1 - 1) {
                                    newConceptOfCross[count1] = userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]];
                                    count1++;
                                }
                            }//of if
                        }//of for k
                    }//of if
                } else {
                    if (count1 == 0) {
                        newConceptOfCross[count1] = userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]];
                        count1++;
                    } else {
                        boolean isSame;
                        for (int k = 0; k < count1; k++) {
                            isSame = isConceptSame(userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]], newConceptOfCross[k]);
                            if (isSame) {
                                break;
                            } else {
                                if (k == count1 - 1) {
                                    newConceptOfCross[count1] = userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]];
                                    count1++;
                                }//of if
                            }//of if
                        }//of for k
                    }//of if
                }//of if
                // generate new concept by cross
                Concept tempNewConcept = ratingMatrix.getNewConceptByCrossUsers(
                        userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]],
                        userOrientedConcepts[userConceptInclusionMatrix[paraUser][j]]);
                // is the new concept effective?
                if ((tempNewConcept.users.length > 1) && (tempNewConcept.items.length > 0)) {
                    // is the new concept better than the old concept?
                    if (ratingMatrix.computeSimilarityOfConcepts(tempNewConcept) > Math.min(ratingMatrix.computeSimilarityOfConcepts(userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]]), ratingMatrix.computeSimilarityOfConcepts(userOrientedConcepts[userConceptInclusionMatrix[paraUser][j]]))) {
                        // is the concept same as before
                        // true means same,false means not same
                        if (count1 == 0) {
                            // record the concept
                            newConceptOfCross[count1] = tempNewConcept;
                            System.out.println("After cross(0): " + Arrays.toString(newConceptOfCross[count1].users)
                                    + Arrays.toString(newConceptOfCross[count1].items));
                            count1++;
                        } else {
                            boolean isSame;
                            for (int k = 0; k < count1; k++) {
                                isSame = isConceptSame(tempNewConcept, newConceptOfCross[k]);
                                if (isSame) {
                                    break;
                                } else {
                                    if (k == count1 - 1) {
                                        // record the new concept
                                        newConceptOfCross[count1] = tempNewConcept;
                                        System.out.println("After cross: " + Arrays.toString(newConceptOfCross[count1].users)
                                                + Arrays.toString(newConceptOfCross[count1].items));
                                        count1++;
                                    } // of if
                                } // of if
                            } // of for k
                        } // of if
                    } // of if
                } // of if
            } // of for j
        } // of for i

        // mutate
        int count2 = 0;
        for (int i = 0; i < count1; i++) {
            if (newConceptOfCross[i].users.length <= paraLowUserThreshold) {

                // generate a concept by mutate
                Concept tempNewConcept = ratingMatrix.mutateOrientedConcepts(paraUser, paraItemThreshold,
                        newConceptOfCross[i]);
                if (count2 == 0) {
                    // record the concept
                    newConcept[count2] = tempNewConcept;

                    System.out.println("After mutate(0): " + Arrays.toString(newConcept[count2].users)
                            + Arrays.toString(newConcept[count2].items));
                    count2++;
                } else {
                    // is the concept same
                    // true means same,false means not same
                    boolean isSame;
                    for (int k = 0; k < count2; k++) {
                        isSame = isConceptSame(tempNewConcept, newConcept[k]);
                        if (isSame) {
                            break;
                        } else {
                            if (k == count2 - 1) {
                                // record the new concept
                                newConcept[count2] = tempNewConcept;
                                System.out.println("After mutate: " + Arrays.toString(newConcept[count2].users)
                                        + Arrays.toString(newConcept[count2].items));
                                count2++;
                            } // of if
                        } // of if
                    } // of for k
                } // of if
            } else {
                newConcept[count2] = newConceptOfCross[i];
                count2++;
            }//of if
        } // of for i
        // final
//        if (count2 != 0) {
//            recordUsers[paraUser] = true;
//        }//of if
        //use old concept to recommend
//		Concept[] userConcepts = new Concept[userConceptCounts[paraUser]];
//		for (int i = 0; i < userConceptCounts[paraUser]; i++) {
//			userConcepts[i] = userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]];
//		}//of for i
//		int[] tempRecommendation;
//		tempRecommendation = recommendationTest(paraUser,0.5,userConcepts);
//		for (int j = 0; j < tempRecommendation.length; j++) {
//			recommendations[paraUser][j] = tempRecommendation[j];
//		} // of for j
        //use new concept to recommend
        if (count2 != 0) {
            Concept[] finalConcept = new Concept[count2];
            for (int i = 0; i < finalConcept.length; i++) {
                finalConcept[i] = newConcept[i];
            }//of for i
            int[][] tempRecommendations;
            tempRecommendations = recommendationToAllUsers(0.5, finalConcept);
            for (int i = 0; i < tempRecommendations.length; i++) {
                for (int j = 0; j < tempRecommendations[0].length; j++) {
//					if (recommendations[i][j] == 0) {
                    recommendations[i][j] += tempRecommendations[i][j];
//					}//of if
                }//of for j
            }//of for i
        }//of if

    }// of GCGAv6

    /**
     * ACGA
     *
     * @param paraUser
     * @param paraLowUserThreshold
     * @param paraItemThreshold
     */
    public void GCGAv7(int paraUser, int paraLowUserThreshold, int paraItemThreshold) {
        // Cross
        Concept[] newConceptOfCross = new Concept[600];
        int count1 = 0;
        for (int i = 0; i < tempConceptCounts[paraUser] - 1; i++) {
            for (int j = i + 1; j < tempConceptCounts[paraUser]; j++) {
                // generate new concept by cross
//                System.out.println("user: " + paraUser);
//                System.out.println("userOrientedConcepts " + userConceptInclusionMatrix[paraUser][i] + Arrays.toString(userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]].users));
//                System.out.println("userOrientedConcepts " + userConceptInclusionMatrix[paraUser][j] + Arrays.toString(userOrientedConcepts[userConceptInclusionMatrix[paraUser][j]].users));
                Concept tempNewConcept = ratingMatrix.getNewConceptByCrossUsers(
                        userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]],
                        userOrientedConcepts[userConceptInclusionMatrix[paraUser][j]]);
                // is the new concept effective?
                if ((tempNewConcept.users.length > 1) && (tempNewConcept.items.length > 0)) {
                    // is the new concept better than the old concept?
                    if (ratingMatrix.computeSimilarityOfConcepts(tempNewConcept) > Math.min(ratingMatrix.computeSimilarityOfConcepts(userOrientedConcepts[userConceptInclusionMatrix[paraUser][i]]), ratingMatrix.computeSimilarityOfConcepts(userOrientedConcepts[userConceptInclusionMatrix[paraUser][j]]))) {
                        // is the concept same as before
                        // true means same,false means not same
                        if (count1 == 0) {
                            newConceptOfCross[count1] = tempNewConcept;
                            count1++;
                        } else {
                            boolean isSame;
                            for (int k = 0; k < count1; k++) {
                                isSame = isConceptSame(tempNewConcept, newConceptOfCross[k]);
                                if (isSame) {
                                    break;
                                } else {
                                    if (k == count1 - 1) {
                                        // record the new concept
                                        newConceptOfCross[count1] = tempNewConcept;
                                        System.out.println("After cross: " + Arrays.toString(newConceptOfCross[count1].users)
                                                + Arrays.toString(newConceptOfCross[count1].items));
                                        count1++;
                                    } // of if
                                } // of if
                            } // of for k
                        }//of if
                    } // of if
                } // of if
            } // of for j
        } // of for i
        // mutate
        for (int i = 0; i < count1; i++) {
            if (newConceptOfCross[i].users.length <= paraLowUserThreshold) {
                // generate a concept by mutate
                Concept tempNewConcept = ratingMatrix.mutateOrientedConcepts(paraUser, paraItemThreshold,
                        newConceptOfCross[i]);
//                // is the concept same
//                // true means same,false means not same
                boolean isSame;
                for (int k = 0; k < userConceptCounts[paraUser]; k++) {
                    isSame = isConceptSame(tempNewConcept, userOrientedConcepts[userConceptInclusionMatrix[paraUser][k]]);
                    if (isSame) {
                        break;
                    } else {
                        if (k == userConceptCounts[paraUser] - 1) {
                            // record the new concept
                            userOrientedConcepts[numNewConcept] = tempNewConcept;
                            for (int j = 0; j < tempNewConcept.users.length; j++) {
                                userConceptInclusionMatrix[tempNewConcept.users[j]][userConceptCounts[tempNewConcept.users[j]]] = numNewConcept;
                                userConceptCounts[tempNewConcept.users[j]]++;
                            }
                            numNewConcept++;
                            System.out.println("After mutate: " + Arrays.toString(tempNewConcept.users)
                                    + Arrays.toString(tempNewConcept.items));
//                            count2++;
                        } // of if
                    } // of if
                } // of for k
//
            } else {
                userOrientedConcepts[numNewConcept] = newConceptOfCross[i];
                for (int j = 0; j < newConceptOfCross[i].users.length; j++) {
                    userConceptInclusionMatrix[newConceptOfCross[i].users[j]][userConceptCounts[newConceptOfCross[i].users[j]]] = numNewConcept;
                    userConceptCounts[newConceptOfCross[i].users[j]]++;
                }//of for j
                numNewConcept++;
            }//of if
        } // of for i

    }// of GCGAv7

    /**
     * Validate the recommendation while this rating matrix serves as the test set.
     * Record the contribution of each concept.
     *
     * @param paraTestMatrix
     * @param paraRecordUsers
     * @param paraRecommendations The recommendation in matrix, storing recommended concept index
     *                            above -1 indicates recommend, -1 indicates not recommend.
     * @return
     */
    public double[] validateRecommendationOfGCGA(boolean[][] paraTestMatrix, boolean[] paraRecordUsers, int[][] paraRecommendations) {
        double tempRecommendation = 0;
        double tempCorrect = 0;
        double tempRated = 0;
        double tempTP = 0;
        double tempFP = 0;
        double tempTN = 0;
        double tempFN = 0;
        double[] tempResult = new double[3];
        for (int i = 0; i < paraRecordUsers.length; i++) {
            if (paraRecordUsers[i]) {
                for (int j = 0; j < paraRecommendations[i].length; j++) {
                    if ((paraRecommendations[i][j] == 0) && (!paraTestMatrix[i][j])) {
                        tempTN++;
                    } else if ((paraRecommendations[i][j] == 0) && (paraTestMatrix[i][j])) {
                        tempFP++;
                        tempRated++;
                    } else if ((paraRecommendations[i][j] > 0) && (!paraTestMatrix[i][j])) {
                        tempFN++;
                        tempRecommendation++;
                    } else if ((paraRecommendations[i][j] > 0) && (paraTestMatrix[i][j])) {
                        tempTP++;
                        tempRecommendation++;
                        tempRated++;
                        tempCorrect++;
                    } // Of if
                } // of for j
            } // of if
        } // of for i
        System.out.println("tempCorrect: " + tempCorrect);
        System.out.println("tempRecommendation: " + tempRecommendation);
        System.out.println("tempRated: " + tempRated);
        double tempPrecision = tempCorrect / tempRecommendation;
        double tempRecall = tempCorrect / tempRated;
        tempResult[0] = tempPrecision;
        tempResult[1] = tempRecall;
        tempResult[2] = (2 * tempPrecision * tempRecall) / (tempPrecision + tempRecall);
        System.out.println("Precision = " + tempPrecision + " recall = " + tempRecall + " F1 = "
                + (2 * tempPrecision * tempRecall) / (tempPrecision + tempRecall));
        double TPR = tempTP / (tempTP + tempFN);
        double FPR = tempFP / (tempFP + tempTN);
        System.out.println("TPR: " + TPR);
        System.out.println("FPR: " + FPR);
        return tempResult;
    }// Of validateRecommendationOfGCGAv6

    /**
     * Concept set based recommendation.
     *
     * @param paraThreshold
     * @return
     */
    public int[][] recommendation(double paraThreshold) {
        int[][] resultRecommendations = new int[numUsers][numItems];
        boolean[] tempRecommendations;
        for (int i = 0; i < numUsers; i++) {
            for (int j = 0; j < userConceptCounts[i]; j++) {
                tempRecommendations = ratingMatrix.userConceptBasedRecommendation(i,
                        userOrientedConcepts[userConceptInclusionMatrix[i][j]], paraThreshold);
                for (int k = 0; k < numItems; k++) {
                    if (tempRecommendations[k]) {
                        resultRecommendations[i][k]++;
                    } // of if
                } // of for k
            } // of for j
        } // of for i
        for (int i = 0; i < resultRecommendations.length; i++) {
            for (int j = 0; j < resultRecommendations[0].length; j++) {
                if (resultRecommendations[i][j] < 2) {
                    resultRecommendations[i][j] = 0;
                }
            }
        }
        return resultRecommendations;
    }// Of recommendation

    /**
     * Validate the recommendation while this rating matrix serves as the test set.
     * Record the contribution of each concept.
     *
     * @param paraTestMatrix
     * @param paraRecommendations The recommendation in matrix, storing recommended concept index
     *                            above -1 indicates recommend, -1 indicates not recommend.
     * @return
     */
    public double[] validateRecommendation(boolean[][] paraTestMatrix, int[][] paraRecommendations) {
        double tempRecommendation = 0;
        double tempCorrect = 0;
        double tempRated = 0;
        double tempTP = 0;
        double tempFP = 0;
        double tempTN = 0;
        double tempFN = 0;
        double[] tempResult = new double[3];
        for (int i = 0; i < paraRecommendations.length; i++) {
            // if (userConceptCountsOfCrossOver[i] != 0) {
            for (int j = 0; j < paraRecommendations[0].length; j++) {
                if ((paraRecommendations[i][j] == 0) && (!paraTestMatrix[i][j])) {
                    tempTN++;
                } else if ((paraRecommendations[i][j] == 0) && (paraTestMatrix[i][j])) {
                    tempFP++;
                    tempRated++;
                } else if ((paraRecommendations[i][j] > 0) && (!paraTestMatrix[i][j])) {
                    tempFN++;
                    tempRecommendation++;
                } else if ((paraRecommendations[i][j] > 0) && (paraTestMatrix[i][j])) {
                    tempTP++;
                    tempRecommendation++;
                    tempRated++;
                    tempCorrect++;
                } // Of if
            } // Of for j
            // } // of if
        } // Of for i
        System.out.println("tempCorrect: " + tempCorrect);
        System.out.println("tempRecommendation: " + tempRecommendation);
        System.out.println("tempRated: " + tempRated);
        double tempPrecision = tempCorrect / tempRecommendation;
        double tempRecall = tempCorrect / tempRated;
        tempResult[0] = tempPrecision;
        tempResult[1] = tempRecall;
        tempResult[2] = (2 * tempPrecision * tempRecall) / (tempPrecision + tempRecall);
        System.out.println("Precision = " + tempPrecision + " recall = " + tempRecall + " F1 = "
                + (2 * tempPrecision * tempRecall) / (tempPrecision + tempRecall));
//		double TPR = tempTP / (tempTP + tempFN);
//		double FPR = tempFP / (tempFP + tempTN);
//		System.out.println("TPR: " + TPR);
//		System.out.println("FPR: " + FPR);
        return tempResult;
    }// Of validateRecommendation

    /**
     * Validate the recommendation while this rating matrix serves as the test set.
     * Record the contribution of each concept.
     *
     * @param paraTestMatrix
     * @param paraRecommendations The recommendation in matrix, storing recommended concept index
     *                            above -1 indicates recommend, -1 indicates not recommend.
     * @return
     */
    public double[] validateRecommendationTest(boolean[][] paraTestMatrix, int[] paraRecommendations) {
        double tempRecommendation = 0;
        double tempCorrect = 0;
        double tempRated = 0;
        double[] tempResult = new double[4];

        for (int i = 0; i < paraRecommendations.length; i++) {
            if ((paraRecommendations[i] == 0) && (!paraTestMatrix[0][i])) {
            } else if ((paraRecommendations[i] == 0) && (paraTestMatrix[0][i])) {
                tempRated++;
            } else if ((paraRecommendations[i] > 0) && (!paraTestMatrix[0][i])) {
                tempRecommendation++;
            } else if ((paraRecommendations[i] > 0) && (paraTestMatrix[0][i])) {
                tempRecommendation++;
                tempRated++;
                tempCorrect++;
            } // Of if
        } // Of for i
        System.out.println("tempCorrect: " + tempCorrect);
        System.out.println("tempRecommendation: " + tempRecommendation);
        System.out.println("tempRated: " + tempRated);
        double tempPrecision = tempCorrect / tempRecommendation;
        double tempRecall = tempCorrect / tempRated;
        tempResult[0] = tempPrecision;
        tempResult[1] = tempRecall;
        tempResult[2] = (2 * tempPrecision * tempRecall) / (tempPrecision + tempRecall);
        System.out.println("Precision = " + tempPrecision + " recall = " + tempRecall + " F1 = "
                + (2 * tempPrecision * tempRecall) / (tempPrecision + tempRecall));
        return tempResult;
    }// Of validateRecommendationTest

    /**
     * Recommendation test.
     *
     * @param paraThreshold
     * @param paraConceptsSet
     * @return
     */
    public int[] recommendationTest(int paraUser, double paraThreshold, Concept[] paraConceptsSet) {
        int[] resultRecommendations = new int[numItems];
        boolean[] tempRecommendations;
        for (int i = 0; i < paraConceptsSet.length; i++) {
            tempRecommendations = ratingMatrix.userConceptBasedRecommendation(paraUser, paraConceptsSet[i],
                    paraThreshold);
            // System.out.println("conceptsSet " + i + ":" + Arrays.toString(tempRecommendations));
            for (int j = 0; j < numItems; j++) {
                if (tempRecommendations[j]) {
                    resultRecommendations[j]++;
                } // of if
            } // of for k
        } // of for j2
        return resultRecommendations;
    }// of recommendationTest

    /**
     * Recommendation test to all users.
     *
     * @param paraThreshold
     * @param paraConceptsSet
     * @return
     */
    public int[][] recommendationToAllUsers(double paraThreshold, Concept[] paraConceptsSet) {
        int[][] resultRecommendations = new int[numUsers][numItems];
        boolean[] tempRecommendations;
        for (int i = 0; i < paraConceptsSet.length; i++) {
            for (int j = 0; j < paraConceptsSet[i].users.length; j++) {
                tempRecommendations = ratingMatrix.userConceptBasedRecommendation(paraConceptsSet[i].users[j],
                        paraConceptsSet[i], paraThreshold);
                for (int j2 = 0; j2 < tempRecommendations.length; j2++) {
                    if (tempRecommendations[j2]) {
                        resultRecommendations[paraConceptsSet[i].users[j]][j2]++;
                    } // of if
                } // of for j2
            } // of for j
        } // of for i
        return resultRecommendations;
    }// of recommendationTest

    /**
     * Compute Sparsity.
     */
    public void computeSparsity() {
        double sparsity = 0;
        double count1 = 0;
        double count2 = 0;
        for (int i = 0; i < ratingMatrix.formalContext.length; i++) {
            for (int j = 0; j < ratingMatrix.formalContext[0].length; j++) {
                if (ratingMatrix.formalContext[i][j]) {
                    count1++;
                }//of if
                count2++;
            }//of for j
        }//of for i
        sparsity = count1 / count2;
        System.out.println("Data set sparsity: " + sparsity);
    }//of computeSparsity

    /**
     * Train and test 1.
     */
    public static void trainAndTest1() {
        RatingMatrix tempTrainRatingMatrix = new RatingMatrix("src/data/training.arff", 943, 1682);
        RatingMatrix tempTestRatingMatrix = new RatingMatrix("src/data/testing.arff", 943, 1682);

        ConceptSet tempSet = new ConceptSet(tempTrainRatingMatrix);
        tempSet.generateDeConceptSet(2, 5);
//		tempSet.showInformation();
//      long startTime = System.currentTimeMillis();
//        for (int i = 0; i < tempSet.numUsers; i++) {
//            System.out.println("for user " + i + " ");
//            tempSet.GCGAv6(i, 2, 1);
//        } // of for i
//        tempSet.GCGAv7(275, 2, 1);
        for (int i = 25; i < tempSet.numUsers; i++) {
            System.out.println("for user " + i + " ");
            tempSet.GCGAv7(i, 5, 1);
        } // of for i
        int[][] tempRecommendations = tempSet.recommendation(0.5);
        tempSet.validateRecommendation(tempTestRatingMatrix.formalContext, tempRecommendations);
//      long endTime = System.currentTimeMillis();
//        tempSet.validateRecommendation(tempTestRatingMatrix.formalContext, recommendations);
//		tempSet.validateRecommendationOfGCGAv6(tempTestRatingMatrix.formalContext, recordUsers, recommendations);
//      System.out.println("Time: " + (endTime - startTime) + "ms");
    }// Of trainAndTest

    /**
     * Train and test 2.
     */
    public static void trainAndTest2() {
        String tempTrainFormalContextURL = "src/data/smalltest.txt";
        String tempTestFormalContextURL = "src/data/smalltest.txt";

        RatingMatrix tempTrainRatingMatrix = new RatingMatrix(10, 7, tempTrainFormalContextURL);
        RatingMatrix tempTestRatingMatrix = new RatingMatrix(10, 7, tempTestFormalContextURL);

        ConceptSet tempSet = new ConceptSet(tempTrainRatingMatrix);
        for (int i = 0; i < tempTestRatingMatrix.formalContext.length; i++) {
            int sum = 0;
            int count = 0;
            double sim = 0;
            for (int j = 0; j < tempTestRatingMatrix.formalContext[0].length; j++) {
                if (tempTestRatingMatrix.formalContext[2][j] && tempTestRatingMatrix.formalContext[i][j]) {
                    sum += Math.abs(tempTestRatingMatrix.ratingMatrix[2][j] - tempTestRatingMatrix.ratingMatrix[i][j]);
                    count++;
                }
            }//of for j
            sim = sum / count;
            System.out.println("2" + "," + i + ":" + sim);
        }//of for i
//		tempSet.generateConcept(2, 2);
//		tempSet.computeSparsity();

//		long startTime1 = System.currentTimeMillis();
        tempSet.generateDeConceptSet(2, 2);
//		System.out.println("for old concepts");
//		int[][] tempRecommendation;
//		tempRecommendation = tempSet.recommendation(0.5);
//		tempSet.validateRecommendation(tempTestRatingMatrix.formalContext, tempRecommendation);
//		long endTime1 = System.currentTimeMillis();
//		System.out.println("Time: " + (endTime1 - startTime1) + "ms");
//		
//		long startTime2 = System.currentTimeMillis();
////		System.out.println("for new concepts");
//		for (int i = 0; i < tempTrainRatingMatrix.formalContext.length; i++) {
//			tempSet.GCGAv6(i, 2, 1);
//		} // of for i
//
//		tempSet.validateRecommendation(tempTestRatingMatrix.formalContext, recommendations);
//		long endTime2 = System.currentTimeMillis();
//		System.out.println("Time: " + (endTime2 - startTime2) + "ms");
//		tempSet.validateRecommendationOfGCGA(tempTestRatingMatrix.formalContext, recordUsers, recommendations);

    }// Of trainAndTest2

    /**
     * Train and test 3.
     */
    public static void trainAndTest3() {
        String tempTrainFormalContextURL = "src/data/jester-data-1-train.txt";
        String tempTestFormalContextURL = "src/data/jester-data-1-test.txt";

        RatingMatrix tempTrainRatingMatrix = new RatingMatrix(24983, 100, tempTrainFormalContextURL);
        RatingMatrix tempTestRatingMatrix = new RatingMatrix(24983, 100, tempTestFormalContextURL);

        ConceptSet tempSet = new ConceptSet(tempTrainRatingMatrix);

        tempSet.computeSparsity();

        long startTime1 = System.currentTimeMillis();
        tempSet.generateDeConceptSet(2, 2);
        System.out.println("for old concepts");
        int[][] tempRecommendation;
        tempRecommendation = tempSet.recommendation(0.5);
        tempSet.validateRecommendation(tempTestRatingMatrix.formalContext, tempRecommendation);
        long endTime1 = System.currentTimeMillis();
        System.out.println("Time: " + (endTime1 - startTime1) + "ms");

        long startTime2 = System.currentTimeMillis();
        System.out.println("for new concepts");
        for (int i = 0; i < tempTrainRatingMatrix.formalContext.length; i++) {
            tempSet.GCGAv6(i, 2, 1);
        } // of for i

        tempSet.validateRecommendation(tempTestRatingMatrix.formalContext, recommendations);
        long endTime2 = System.currentTimeMillis();
        System.out.println("Time: " + (endTime2 - startTime2) + "ms");
//		tempSet.validateRecommendationOfGCGA(tempTestRatingMatrix.formalContext, recordUsers, recommendations);
    }// Of trainAndTest3

    /**
     * Train and test 4.
     */
    public static void trainAndTest4() {
        String tempTrainFormalContextURL = "src/data/movielens1m-Train0.2.txt";
        String tempTestFormalContextURL = "src/data/movielens1m-Test0.2.txt";

        RatingMatrix tempTrainRatingMatrix = new RatingMatrix(6040, 3952, tempTrainFormalContextURL);
        RatingMatrix tempTestRatingMatrix = new RatingMatrix(6040, 3952, tempTestFormalContextURL);

        ConceptSet tempSet = new ConceptSet(tempTrainRatingMatrix);
        tempSet.computeSparsity();

        long startTime1 = System.currentTimeMillis();
        tempSet.generateDeConceptSet(2, 2);
//		System.out.println("for old concepts");
        int[][] tempRecommendation = tempSet.recommendation(0.5);
        tempSet.validateRecommendation(tempTestRatingMatrix.formalContext, tempRecommendation);
        long endTime1 = System.currentTimeMillis();
//		System.out.println("Time: " + (endTime1 - startTime1) + "ms");
//		
        long startTime2 = System.currentTimeMillis();
//		System.out.println("for new concepts");
        for (int i = 0; i < tempTrainRatingMatrix.formalContext.length; i++) {
            System.out.println("for user " + i + ":");
            tempSet.GCGAv6(i, 2, 1);
        } // of for i
//
        tempSet.validateRecommendation(tempTestRatingMatrix.formalContext, recommendations);
        long endTime2 = System.currentTimeMillis();
        System.out.println("Time: " + (endTime2 - startTime2) + "ms");
    }

    /**
     * Train and test 5.
     */
    public static void trainAndTest5() {
//		String tempTrainFormalContextURL = "src/data/movielens1m-Train0.2.txt";
//		String tempTestFormalContextURL = "src/data/movielens1m-Test0.2.txt";
//		
//		RatingMatrix tempTrainRatingMatrix = new RatingMatrix(6040, 3952, tempTrainFormalContextURL);
//		RatingMatrix tempTestRatingMatrix = new RatingMatrix(6040, 3952, tempTestFormalContextURL);
        String tempTrainFormalContextURL = "src/data/eachMovie-2k-CompressedTrain0.3.txt";
        String tempTestFormalContextURL = "src/data/eachMovie-2k-CompressedTest0.3.txt";
        RatingMatrix tempTrainRatingMatrix = new RatingMatrix(2000, 1648, tempTrainFormalContextURL);
        RatingMatrix tempTestRatingMatrix = new RatingMatrix(2000, 1648, tempTestFormalContextURL);
        ConceptSet tempSet = new ConceptSet(tempTrainRatingMatrix);
//		tempSet.sampling();
//		tempSet.dividTrainAndTest();
        tempSet.computeSparsity();

//		long startTime1 = System.currentTimeMillis();
        tempSet.generateDeConceptSet(2, 2);
        int[][] tempRecommendations = tempSet.recommendation(0.5);
        tempSet.validateRecommendation(tempTestRatingMatrix.formalContext, tempRecommendations);
//		System.out.println("for old concepts");
//		int[][] tempRecommendation;
//		tempRecommendation = tempSet.recommendation(0.5);
//		tempSet.validateRecommendation(tempTestRatingMatrix.formalContext, tempRecommendation);
//		long endTime1 = System.currentTimeMillis();
//		System.out.println("Time: " + (endTime1 - startTime1) + "ms");
//		
//		long startTime2 = System.currentTimeMillis();
//		System.out.println("for new concepts");
//		for (int i = 0; i < tempTrainRatingMatrix.formalContext.length; i++) {
//			System.out.println("for user " + i  + ":");
//			tempSet.GCGAv6(i, 2, 1);
//		} // of for i
//
//		tempSet.validateRecommendation(tempTestRatingMatrix.formalContext, recommendations);
//		long endTime2 = System.currentTimeMillis();
//		System.out.println("Time: " + (endTime2 - startTime2) + "ms");
    }

    public static void test() {
        String tempTrainFormalContextURL = "src/data/smalltest.txt";
        String tempTestFormalContextURL = "src/data/smalltest.txt";

        RatingMatrix tempTrainRatingMatrix = new RatingMatrix(10, 7, tempTrainFormalContextURL);
        RatingMatrix tempTestRatingMatrix = new RatingMatrix(10, 7, tempTestFormalContextURL);

        ConceptSet tempSet = new ConceptSet(tempTrainRatingMatrix);
        tempSet.generateDeConceptSet(2, 2);
        for (int i = 0; i < tempTrainRatingMatrix.formalContext.length; i++) {
            System.out.println("for user " + i + ":");
            tempSet.GCGAv6(i, 2, 1);
        } // of for i
    }

    /**
     * Entrance of this program.
     *
     * @param args
     * @throws FileNotFoundException
     */
    public static void main(String args[]) throws FileNotFoundException {
        trainAndTest1();
//		trainAndTest2();
//		trainAndTest3();
//		trainAndTest4();
//        trainAndTest5();
//        test();
    }// Of main


}// Of ConceptSet
