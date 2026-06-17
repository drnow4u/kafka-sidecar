import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Minimal GitVersion replacement. Requires only git on PATH.
 *
 * Usage: java GitVersion.java
 * Output: semantic version string, e.g. "0.7.30" or "0.7.30-my-feature"
 *
 * Rules:
 *  - If HEAD is already tagged with a semver tag → return that tag (post-release use, e.g. Makefile)
 *  - Otherwise find nearest reachable semver tag, bump patch, append branch slug for non-main branches
 */
public class GitVersion {

    private static final String SEMVER_PATTERN = "\\d+\\.\\d+\\.\\d+";
    private static final List<String> MAIN_BRANCHES = List.of("main", "master");

    public static void main(String[] args) throws Exception {
        // If HEAD is already tagged return it as-is (Makefile / docker tagging use case)
        var headTags = git("tag", "--points-at", "HEAD", "--list", "--sort=-version:refname");
        var headTag = Arrays.stream(headTags.split("\n"))
                .map(String::trim)
                .filter(t -> t.matches(SEMVER_PATTERN))
                .findFirst();

        if (headTag.isPresent()) {
            System.out.println(headTag.get());
            return;
        }

        // Find nearest reachable semver tag
        String nearestTag;
        try {
            nearestTag = git("describe", "--tags", "--abbrev=0", "--match", "[0-9]*").trim();
        } catch (Exception e) {
            nearestTag = "0.0.0";
        }

        var next = bumpPatch(nearestTag);
        var branch = git("rev-parse", "--abbrev-ref", "HEAD").trim();

        if (MAIN_BRANCHES.contains(branch) || "HEAD".equals(branch)) {
            System.out.println(next);
        } else {
            var commitCount = git("rev-list", nearestTag + "..HEAD", "--count").trim();
            var slug = slugify(branch);
            System.out.println(next + "-" + slug + "." + commitCount);
        }
    }

    private static String bumpPatch(String tag) {
        var semver = tag.replaceAll("-.*$", ""); // strip pre-release suffix e.g. "-bumpup.1-SNAPSHOT"
        var parts = semver.split("\\.");
        var major = Integer.parseInt(parts[0]);
        var minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        var patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return major + "." + minor + "." + (patch + 1);
    }

    private static String slugify(String branch) {
        return branch.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private static String git(String... args) throws Exception {
        var cmd = new ArrayList<String>();
        cmd.add("git");
        cmd.addAll(List.of(args));
        var process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        var output = new String(process.getInputStream().readAllBytes()).trim();
        if (process.waitFor() != 0) throw new RuntimeException("git: " + output);
        return output;
    }
}