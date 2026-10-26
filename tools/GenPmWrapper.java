import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** Prints all abstract instance methods of android.content.pm.PackageManager (for generating a delegating wrapper). */
public class GenPmWrapper {
    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        for (Method m : android.content.pm.PackageManager.class.getDeclaredMethods()) {
            int mod = m.getModifiers();
            if (Modifier.isAbstract(mod) && !Modifier.isStatic(mod)) {
                StringBuilder sb = new StringBuilder();
                sb.append(m.getReturnType().getCanonicalName().replace('$', '.'))
                  .append('|').append(m.getName());
                List<String> params = new ArrayList<>();
                for (Class<?> p : m.getParameterTypes()) params.add(p.getCanonicalName().replace('$', '.'));
                sb.append('|').append(String.join(",", params));
                List<String> ex = new ArrayList<>();
                for (Class<?> e : m.getExceptionTypes()) ex.add(e.getCanonicalName().replace('$', '.'));
                sb.append('|').append(String.join(",", ex));
                lines.add(sb.toString());
            }
        }
        System.out.println(String.join("\n", lines));
    }
}
