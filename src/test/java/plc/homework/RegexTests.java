package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. Test structure for steps 1 & 2 are
 * provided, you must create this yourself for step 3.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas? gasoline
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("whitespace included", "hello everyone", true),
                Arguments.of("20 Characters", "isthisevenworking???", true),
                Arguments.of("12 characters", "honey beeeee", true),
                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "goodnightmoon", false),
                Arguments.of("empty string", "", false),
                Arguments.of("15 characters", "thirteen flames", false),
                Arguments.of("21 Characters", "helloWorldhelloWorlds", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,2,3,4]", true),
                Arguments.of("empty list", "[]", true),
                Arguments.of("big numbers", "[1000,20,3785]", true),
                Arguments.of("mixed spacings", "[1,2, 3]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing number between comma", "[1,,3]", false),
                Arguments.of("ending comma", "[1,2,]", false),
                Arguments.of("beginning comma", "[,7,8]", false),
                Arguments.of("Missing Commas", "[1 2 3]", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success);
    }

    public static Stream<Arguments> testNumberRegex() {
        return Stream.of(
                Arguments.of("Is A Number", "45", true),
                Arguments.of("Valid Decimal", "1.23", true),
                Arguments.of("Positive Number", "+33", true),
                Arguments.of("Negative Number", "-78", true),
                Arguments.of("Positive Decimal", "+9.5", true),
                Arguments.of("Is Not A Number", "a", false),
                Arguments.of("Leading Decimal", ".1", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Invalid Sign", "++50", false),
                Arguments.of("Invalid Decimal", "1.89.6", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Basic String", "\"abc123?!@#$%\"", true),
                Arguments.of("String with space", "\"hello world?\"", true),
                Arguments.of("empty string", "\"\"", true),
                Arguments.of("escape character", "\"1\t2\"", true),
                Arguments.of("quote in quotes", "\"hello\"world?\"", true),
                Arguments.of("no opening quote", "happy\"", false),
                Arguments.of("no closing quote", "\"missing", false),
                Arguments.of("misplaced leading quote", "hello \"world?\"", false),
                Arguments.of("misplaced end quote", "\"hello \"world?", false),
                Arguments.of("no quotes", "wrong", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
