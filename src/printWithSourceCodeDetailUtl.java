public class printWithSourceCodeDetailUtl {

    public static final boolean toPrint = false;
    
    public static void println(String message) {
        if (toPrint) {
            String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();

            System.out.println(className + "." + methodName + "(" + lineNumber + ")" + ": " + message);
        }
    }
}
