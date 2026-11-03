
public class sliding {
 
    // ---------- formatting helpers ----------
    static String pad(String s, int w) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < w) sb.append(' ');
        return sb.toString();
    }
 
    static String row(String[] cells, int[] w) {
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < cells.length; i++)
            r.append(i + 1 == cells.length ? cells[i] : pad(cells[i], w[i]));
        return r.toString();
    }
 
    static String toStr(int[] v) {
        StringBuilder s = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) s.append(", ");
            s.append(v[i]);
        }
        return s.append("]").toString();
    }
 
    static String rangeStr(int l, int r) { return "[" + l + ".." + r + "]"; }
 
    static String elems(int[] a, int l, int r) {          // [2,1,5]
        StringBuilder s = new StringBuilder("[");
        for (int i = l; i <= r; i++) {
            if (i > l) s.append(",");
            s.append(a[i]);
        }
        return s.append("]").toString();
    }
 
    static String sliceStr(int[] a, int l, int r) {       // [1, 2, 1]
        StringBuilder s = new StringBuilder("[");
        for (int i = l; i <= r; i++) {
            if (i > l) s.append(", ");
            s.append(a[i]);
        }
        return s.append("]").toString();
    }
 
    // =====================================================================
    // PART A: Maximum sum of a subarray of size K (fixed window)
    // =====================================================================
 
    static void validateFixed(int[] a, int k) {
        if (k < 1 || k > a.length) throw new IllegalArgumentException("K must satisfy 1 <= K <= n");
    }
 
    // Reference: recompute every K-element sum from scratch.
    // Time O(n*K), auxiliary space O(1).
    static long maxSumReference(int[] a, int k) {
        validateFixed(a, k);
        int n = a.length;
        long best = Long.MIN_VALUE;
        for (int start = 0; start + k <= n; start++) {
            long sum = 0;
            for (int j = start; j < start + k; j++) sum += a[j];   // K additions every time
            best = Math.max(best, sum);
        }
        return best;
    }
 
    // Optimized: compute the first window once, then slide:
    //   sum = sum + entering element - leaving element.
    // Time O(n), auxiliary space O(1).
    static long maxSumSlidingWindow(int[] a, int k) {
        validateFixed(a, k);
        int n = a.length;
        long sum = 0;
        for (int i = 0; i < k; i++) sum += a[i];                   // first window, computed once
        long best = sum;
        for (int i = k; i < n; i++) {
            sum += a[i];                                           // entering element a[i]
            sum -= a[i - k];                                       // leaving element a[i-K]
            best = Math.max(best, sum);
        }
        return best;
    }
 
    static void traceFixed(int[] a, int k) {
        System.out.println("Dry run (fixed window) on " + toStr(a) + ", K = " + k);
        int[] w = {6, 10, 13, 10, 10, 7};
        System.out.println(row(new String[]{"Step", "Window", "Elements", "Enter(+)", "Leave(-)", "Sum", "Max so far"}, w));
        int n = a.length;
        long sum = 0;
        for (int i = 0; i < k; i++) sum += a[i];
        long best = sum;
        System.out.println(row(new String[]{"init", rangeStr(0, k - 1), elems(a, 0, k - 1), "-", "-",
                String.valueOf(sum), String.valueOf(best)}, w));
        for (int i = k; i < n; i++) {
            int enter = a[i], leave = a[i - k];
            sum += enter;
            sum -= leave;
            best = Math.max(best, sum);
            System.out.println(row(new String[]{String.valueOf(i - k + 1), rangeStr(i - k + 1, i),
                    elems(a, i - k + 1, i), String.valueOf(enter), String.valueOf(leave),
                    String.valueOf(sum), String.valueOf(best)}, w));
        }
        System.out.println("  Result: maximum sum = " + best + "\n");
    }
 
    static void runFixedTests() {
        int[][] arrays = {{2, 1, 5, 1, 3, 2}, {-4, -2, -7}, {6}, {1, 2, 3}, {3, -1, 4}, {-2, 3, -1, 2}};
        int[] ks = {3, 2, 1, 3, 1, 2};
        long[] expected = {9, -6, 6, 6, 4, 2};
        int[] w = {5, 22, 5, 10, 11, 16};
        System.out.println("Test results (fixed window):");
        System.out.println(row(new String[]{"No.", "Array", "K", "Expected", "Reference", "Sliding window", "Result"}, w));
        for (int t = 0; t < arrays.length; t++) {
            long ref = maxSumReference(arrays[t], ks[t]);
            long opt = maxSumSlidingWindow(arrays[t], ks[t]);
            boolean ok = (ref == expected[t] && opt == expected[t]);
            System.out.println(row(new String[]{String.valueOf(t + 1), toStr(arrays[t]), String.valueOf(ks[t]),
                    String.valueOf(expected[t]), String.valueOf(ref), String.valueOf(opt), ok ? "PASS" : "FAIL"}, w));
        }
        System.out.println();
    }
 
    // =====================================================================
    // PART B: Longest subarray with sum <= K (variable window)
    // =====================================================================
 
    // length 0 -> start = end = -1
    static class WinResult {
        int length, start, end;
        WinResult(int length, int start, int end) { this.length = length; this.start = start; this.end = end; }
    }
 
    static void validateVariable(int[] a, int limit) {
        if (limit < 0) throw new IllegalArgumentException("limit K must be non-negative");
        for (int x : a)
            if (x < 0) throw new IllegalArgumentException("array values must be non-negative");
    }
 
    static String bestStr(WinResult b) {
        if (b.length == 0) return "len=0";
        return "len=" + b.length + " " + rangeStr(b.start, b.end);
    }
 
    // Straightforward: for every start index, extend the end while the sum stays <= K.
    // Because values are non-negative the sum never decreases when extending, so we
    // can stop as soon as it exceeds K.  Time O(n^2), auxiliary space O(1).
    static WinResult longestBruteForce(int[] a, int limit) {
        validateVariable(a, limit);
        int n = a.length;
        WinResult best = new WinResult(0, -1, -1);
        for (int s = 0; s < n; s++) {
            long sum = 0;
            for (int e = s; e < n; e++) {
                sum += a[e];
                if (sum > limit) break;
                int len = e - s + 1;
                if (len > best.length) best = new WinResult(len, s, e);
            }
        }
        return best;
    }
 
    // Optimized: expand right, contract left while sum > K.
    // Each element enters once and leaves at most once -> O(n) time, O(1) space.
    static WinResult longestSlidingWindow(int[] a, int limit) {
        validateVariable(a, limit);
        int n = a.length;
        WinResult best = new WinResult(0, -1, -1);
        long sum = 0;
        int left = 0;
        for (int right = 0; right < n; right++) {
            sum += a[right];                                   // expand: add new right element
            while (sum > limit && left <= right) {             // contract: window too heavy
                sum -= a[left];
                left++;
            }
            int len = right - left + 1;                        // window is valid here (or empty)
            if (len > best.length) best = new WinResult(len, left, right);   // update best
        }
        return best;
    }
 
    static void traceVariable(int[] a, int limit) {
        System.out.println("Dry run (variable window) on " + toStr(a) + ", K = " + limit);
        int[] w = {6, 7, 6, 40, 6, 6};
        System.out.println(row(new String[]{"Step", "Right", "Left", "Action", "Sum", "Len", "Best"}, w));
        int n = a.length;
        WinResult best = new WinResult(0, -1, -1);
        long sum = 0;
        int left = 0, step = 0;
        for (int right = 0; right < n; right++) {
            sum += a[right];
            step++;
            System.out.println(row(new String[]{String.valueOf(step), String.valueOf(right), String.valueOf(left),
                    "expand: add a[" + right + "]=" + a[right],
                    String.valueOf(sum), String.valueOf(right - left + 1), bestStr(best)}, w));
            while (sum > limit && left <= right) {
                int removed = a[left];
                sum -= removed;
                left++;
                step++;
                System.out.println(row(new String[]{String.valueOf(step), String.valueOf(right), String.valueOf(left),
                        "contract: sum > K, remove a[" + (left - 1) + "]=" + removed,
                        String.valueOf(sum), String.valueOf(right - left + 1), bestStr(best)}, w));
            }
            int len = right - left + 1;
            String action;
            if (len > best.length) {
                action = "valid: len " + len + " > best " + best.length + ", update best";
                best = new WinResult(len, left, right);
            } else {
                action = "valid: len " + len + " <= best " + best.length + ", keep best";
            }
            step++;
            System.out.println(row(new String[]{String.valueOf(step), String.valueOf(right), String.valueOf(left),
                    action, String.valueOf(sum), String.valueOf(len), bestStr(best)}, w));
        }
        if (best.length == 0)
            System.out.println("  Result: longest length = 0 (no valid subarray)\n");
        else
            System.out.println("  Result: longest length = " + best.length + ", subarray "
                    + sliceStr(a, best.start, best.end) + " (indices " + best.start + ".." + best.end + ")\n");
    }
 
    static boolean validWindow(int[] a, int limit, WinResult r, int expectedLen) {
        if (r.length != expectedLen) return false;
        if (r.length == 0) return r.start == -1 && r.end == -1;
        if (r.start < 0 || r.end >= a.length || r.end - r.start + 1 != r.length) return false;
        long sum = 0;
        for (int i = r.start; i <= r.end; i++) sum += a[i];
        return sum <= limit;
    }
 
    static void runVariableTests() {
        int[][] arrays = {{1, 2, 1, 1, 3}, {5, 1, 1}, {4, 2, 1}, {0, 0, 1, 0}, {2, 1, 3, 2, 1, 1}, {1, 2, 3}, {7, 8}};
        int[] limits = {4, 2, 0, 1, 5, 10, 5};
        int[] expectedLen = {3, 2, 0, 4, 3, 3, 0};
        String[] expectedText = {"len=3 (0-2 or 1-3)", "len=2 (1-2)", "len=0", "len=4 (0-3)", "len=3 (3-5)", "len=3 (0-2)", "len=0"};
        int[] w = {5, 22, 9, 22, 16, 16};
        System.out.println("Test results (variable window):");
        System.out.println(row(new String[]{"No.", "Array", "Limit K", "Expected", "Brute force", "Sliding window", "Result"}, w));
        for (int t = 0; t < arrays.length; t++) {
            WinResult b = longestBruteForce(arrays[t], limits[t]);
            WinResult o = longestSlidingWindow(arrays[t], limits[t]);
            boolean ok = validWindow(arrays[t], limits[t], b, expectedLen[t])
                      && validWindow(arrays[t], limits[t], o, expectedLen[t]);
            System.out.println(row(new String[]{String.valueOf(t + 1), toStr(arrays[t]), String.valueOf(limits[t]),
                    expectedText[t], bestStr(b), bestStr(o), ok ? "PASS" : "FAIL"}, w));
        }
        System.out.println();
    }
 
    // =====================================================================
    // Input validation checks
    // =====================================================================
 
    static void runValidationChecks() {
        String[] names = {"A: a=[1, 2, 3], K=0", "A: a=[1, 2, 3], K=4 (K > n)", "A: a=[], K=1 (empty array)",
                          "B: a=[1, -2, 3], limit=4", "B: a=[1, 2], limit=-1"};
        Runnable[] calls = {
            () -> maxSumSlidingWindow(new int[]{1, 2, 3}, 0),
            () -> maxSumSlidingWindow(new int[]{1, 2, 3}, 4),
            () -> maxSumSlidingWindow(new int[]{}, 1),
            () -> longestSlidingWindow(new int[]{1, -2, 3}, 4),
            () -> longestSlidingWindow(new int[]{1, 2}, -1),
        };
        int[] w = {5, 32, 12, 52};
        System.out.println("Validation checks:");
        System.out.println(row(new String[]{"No.", "Case", "Expected", "Actual", "Result"}, w));
        for (int i = 0; i < names.length; i++) {
            String actual;
            boolean rejected = false;
            try { calls[i].run(); actual = "accepted"; }
            catch (IllegalArgumentException e) { rejected = true; actual = "rejected: " + e.getMessage(); }
            System.out.println(row(new String[]{String.valueOf(i + 1), names[i], "rejected", actual,
                    rejected ? "PASS" : "FAIL"}, w));
        }
        System.out.println();
    }
 
    public static void main(String[] args) {
        System.out.println("===== PART A: Maximum sum of K consecutive elements (fixed window) =====");
        runFixedTests();
        traceFixed(new int[]{2, 1, 5, 1, 3, 2}, 3);
 
        System.out.println("===== PART B: Longest subarray with sum at most K (variable window) =====");
        runVariableTests();
        traceVariable(new int[]{1, 2, 1, 1, 3}, 4);
        traceVariable(new int[]{5, 1, 1}, 2);
        traceVariable(new int[]{4, 2, 1}, 0);
 
        System.out.println("===== INPUT VALIDATION =====");
        runValidationChecks();
    }
}
 