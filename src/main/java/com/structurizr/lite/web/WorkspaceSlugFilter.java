package com.structurizr.lite.web;

import com.structurizr.lite.Configuration;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lets multi-workspace mode be addressed by a human-readable slug (e.g. /workspace/frameforge)
 * instead of only the numeric workspace ID, by forwarding to the numeric route internally.
 * A workspace directory named "{id}-{slug}" (e.g. "1-frameforge") is addressable as both
 * /workspace/1 and /workspace/frameforge.
 */
public class WorkspaceSlugFilter implements Filter {

    // Matches /workspace/{slug}[...] where {slug} is NOT purely numeric (those are handled
    // natively as workspace IDs) - so a slug like "10ton" (starts with a digit) still works.
    private static final Pattern SLUG_PATH = Pattern.compile("^/workspace/(?!\\d+(?:/.*)?$)([a-zA-Z0-9_-]+)(/.*)?$");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (!Configuration.getInstance().isSingleWorkspace()) {
            Matcher matcher = SLUG_PATH.matcher(path);
            if (matcher.matches()) {
                String slug = matcher.group(1);
                String rest = matcher.group(2) == null ? "" : matcher.group(2);
                Long id = resolveSlugToId(slug);

                if (id != null) {
                    request.getRequestDispatcher("/workspace/" + id + rest).forward(req, res);
                    return;
                }
            }
        }

        chain.doFilter(req, res);
    }

    private Long resolveSlugToId(String slug) {
        File dataDirectory = Configuration.getInstance().getDataDirectory();
        File[] files = dataDirectory.listFiles();
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }

            String name = file.getName();
            int dash = name.indexOf('-');
            if (dash > 0 && name.substring(dash + 1).equalsIgnoreCase(slug)) {
                try {
                    return Long.parseLong(name.substring(0, dash));
                } catch (NumberFormatException e) {
                    // directory name doesn't start with a numeric workspace ID - ignore
                }
            }
        }

        return null;
    }

}
