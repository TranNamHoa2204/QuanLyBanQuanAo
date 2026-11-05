package server.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.persistence.EntityManager;

/** Cấp mã tăng dần theo prefix; cache trong JVM để tránh trùng khi nhiều client cùng thêm. */
final class SequentialCodeGenerator {
    private static final Map<String, Long> LAST_VALUES = new HashMap<>();

    private SequentialCodeGenerator() {
    }

    static synchronized String next(EntityManager em, Class<?> entityType, String idProperty,
            String prefix, int width) {
        String key = entityType.getName() + ":" + idProperty + ":" + prefix;
        Long last = LAST_VALUES.get(key);
        if (last == null) {
            last = findLastValue(em, entityType, idProperty, prefix);
        }
        long next;
        try {
            next = Math.addExact(last, 1L);
        } catch (ArithmeticException overflow) {
            throw ManagementSupport.invalid("Đã vượt giới hạn số thứ tự của mã " + prefix + ".");
        }
        String number = String.format(Locale.ROOT, "%0" + width + "d", next);
        String code = prefix + number;
        if (code.length() > 20) {
            throw ManagementSupport.invalid("Mã " + prefix + " đã vượt quá 20 ký tự.");
        }
        LAST_VALUES.put(key, next);
        return code;
    }

    private static long findLastValue(EntityManager em, Class<?> entityType, String idProperty, String prefix) {
        String jpql = "select e." + idProperty + " from " + entityType.getSimpleName() + " e"
                + " where e." + idProperty + " like :prefix";
        List<?> codes = em.createQuery(jpql)
                .setParameter("prefix", prefix + "%")
                .getResultList();
        long last = 0L;
        for (Object value : codes) {
            if (value instanceof String) {
                String code = (String) value;
                if (code.startsWith(prefix)) {
                    String suffix = code.substring(prefix.length());
                    if (suffix.matches("[0-9]+")) {
                        try {
                            last = Math.max(last, Long.parseLong(suffix));
                        } catch (NumberFormatException tooLarge) {
                            throw ManagementSupport.invalid("Mã " + code + " vượt giới hạn số thứ tự.");
                        }
                    }
                }
            }
        }
        return last;
    }
}
