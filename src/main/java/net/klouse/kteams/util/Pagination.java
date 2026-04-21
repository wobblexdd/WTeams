package net.klouse.kteams.util;

import java.util.Collections;
import java.util.List;

public final class Pagination<T> {

    public List<T> page(List<T> source, int page, int pageSize) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        if (pageSize <= 0) {
            return Collections.emptyList();
        }

        int normalizedPage = Math.max(1, page);
        int fromIndex = (normalizedPage - 1) * pageSize;
        if (fromIndex >= source.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(source.size(), fromIndex + pageSize);
        return source.subList(fromIndex, toIndex);
    }

    public int maxPage(List<T> source, int pageSize) {
        if (source == null || source.isEmpty() || pageSize <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) source.size() / pageSize);
    }
}
