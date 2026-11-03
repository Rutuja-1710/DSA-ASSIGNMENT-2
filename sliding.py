
def row(cells, widths):
    out = ""
    for i, c in enumerate(cells):
        out += c if i == len(cells) - 1 else c.ljust(widths[i])
    return out
 
 
def to_str(v):
    return "[" + ", ".join(str(x) for x in v) + "]"
 
 
def range_str(l, r):
    return f"[{l}..{r}]"
 
 
def elems(a, l, r):          # [2,1,5]
    return "[" + ",".join(str(a[i]) for i in range(l, r + 1)) + "]"
 
 
def slice_str(a, l, r):      # [1, 2, 1]
    return "[" + ", ".join(str(a[i]) for i in range(l, r + 1)) + "]"
 
 
# =====================================================================
# PART A: Maximum sum of a subarray of size K (fixed window)
# =====================================================================
 
def validate_fixed(a, k):
    if k < 1 or k > len(a):
        raise ValueError("K must satisfy 1 <= K <= n")
 
 
def max_sum_reference(a, k):
    """Reference: recompute every K-element sum from scratch.
    Time O(n*K), auxiliary space O(1)."""
    validate_fixed(a, k)
    n = len(a)
    best = None
    for start in range(n - k + 1):
        s = 0
        for j in range(start, start + k):       # K additions every time
            s += a[j]
        if best is None or s > best:
            best = s
    return best
 
 
def max_sum_sliding_window(a, k):
    """Optimized: compute the first window once, then slide:
    sum = sum + entering element - leaving element.
    Time O(n), auxiliary space O(1)."""
    validate_fixed(a, k)
    n = len(a)
    s = 0
    for i in range(k):                          # first window, computed once
        s += a[i]
    best = s
    for i in range(k, n):
        s += a[i]                               # entering element a[i]
        s -= a[i - k]                           # leaving element a[i-K]
        best = max(best, s)
    return best
 
 
def trace_fixed(a, k):
    print(f"Dry run (fixed window) on {to_str(a)}, K = {k}")
    w = [6, 10, 13, 10, 10, 7]
    print(row(["Step", "Window", "Elements", "Enter(+)", "Leave(-)", "Sum", "Max so far"], w))
    n = len(a)
    s = sum(a[:k])
    best = s
    print(row(["init", range_str(0, k - 1), elems(a, 0, k - 1), "-", "-", str(s), str(best)], w))
    for i in range(k, n):
        enter, leave = a[i], a[i - k]
        s += enter
        s -= leave
        best = max(best, s)
        print(row([str(i - k + 1), range_str(i - k + 1, i), elems(a, i - k + 1, i),
                   str(enter), str(leave), str(s), str(best)], w))
    print(f"  Result: maximum sum = {best}\n")
 
 
def run_fixed_tests():
    tests = [
        ([2, 1, 5, 1, 3, 2], 3, 9),
        ([-4, -2, -7], 2, -6),
        ([6], 1, 6),
        ([1, 2, 3], 3, 6),
        ([3, -1, 4], 1, 4),
        ([-2, 3, -1, 2], 2, 2),
    ]
    w = [5, 22, 5, 10, 11, 16]
    print("Test results (fixed window):")
    print(row(["No.", "Array", "K", "Expected", "Reference", "Sliding window", "Result"], w))
    for no, (a, k, exp) in enumerate(tests, start=1):
        ref = max_sum_reference(a, k)
        opt = max_sum_sliding_window(a, k)
        ok = (ref == exp and opt == exp)
        print(row([str(no), to_str(a), str(k), str(exp), str(ref), str(opt), "PASS" if ok else "FAIL"], w))
    print()
 
 
# =====================================================================
# PART B: Longest subarray with sum <= K (variable window)
# =====================================================================
# Results are tuples (length, start, end); length 0 -> start = end = -1.
 
def validate_variable(a, limit):
    if limit < 0:
        raise ValueError("limit K must be non-negative")
    for x in a:
        if x < 0:
            raise ValueError("array values must be non-negative")
 
 
def best_str(b):
    length, start, end = b
    if length == 0:
        return "len=0"
    return f"len={length} {range_str(start, end)}"
 
 
def longest_brute_force(a, limit):
    """Straightforward: for every start index, extend the end while the sum stays <= K.
    Because values are non-negative the sum never decreases when extending, so we
    can stop as soon as it exceeds K.  Time O(n^2), auxiliary space O(1)."""
    validate_variable(a, limit)
    n = len(a)
    best = (0, -1, -1)
    for s in range(n):
        total = 0
        for e in range(s, n):
            total += a[e]
            if total > limit:
                break
            length = e - s + 1
            if length > best[0]:
                best = (length, s, e)
    return best
 
 
def longest_sliding_window(a, limit):
    """Optimized: expand right, contract left while sum > K.
    Each element enters once and leaves at most once -> O(n) time, O(1) space."""
    validate_variable(a, limit)
    n = len(a)
    best = (0, -1, -1)
    total = 0
    left = 0
    for right in range(n):
        total += a[right]                       # expand: add new right element
        while total > limit and left <= right:  # contract: window too heavy
            total -= a[left]
            left += 1
        length = right - left + 1               # window is valid here (or empty)
        if length > best[0]:
            best = (length, left, right)        # update best
    return best
 
 
def trace_variable(a, limit):
    print(f"Dry run (variable window) on {to_str(a)}, K = {limit}")
    w = [6, 7, 6, 40, 6, 6]
    print(row(["Step", "Right", "Left", "Action", "Sum", "Len", "Best"], w))
    n = len(a)
    best = (0, -1, -1)
    total = 0
    left = 0
    step = 0
    for right in range(n):
        total += a[right]
        step += 1
        print(row([str(step), str(right), str(left), f"expand: add a[{right}]={a[right]}",
                   str(total), str(right - left + 1), best_str(best)], w))
        while total > limit and left <= right:
            removed = a[left]
            total -= removed
            left += 1
            step += 1
            print(row([str(step), str(right), str(left),
                       f"contract: sum > K, remove a[{left - 1}]={removed}",
                       str(total), str(right - left + 1), best_str(best)], w))
        length = right - left + 1
        if length > best[0]:
            action = f"valid: len {length} > best {best[0]}, update best"
            best = (length, left, right)
        else:
            action = f"valid: len {length} <= best {best[0]}, keep best"
        step += 1
        print(row([str(step), str(right), str(left), action, str(total), str(length), best_str(best)], w))
    if best[0] == 0:
        print("  Result: longest length = 0 (no valid subarray)\n")
    else:
        print(f"  Result: longest length = {best[0]}, subarray {slice_str(a, best[1], best[2])} "
              f"(indices {best[1]}..{best[2]})\n")
 
 
def valid_window(a, limit, r, expected_len):
    length, start, end = r
    if length != expected_len:
        return False
    if length == 0:
        return start == -1 and end == -1
    if start < 0 or end >= len(a) or end - start + 1 != length:
        return False
    return sum(a[start:end + 1]) <= limit
 
 
def run_variable_tests():
    tests = [
        ([1, 2, 1, 1, 3], 4, 3, "len=3 (0-2 or 1-3)"),
        ([5, 1, 1], 2, 2, "len=2 (1-2)"),
        ([4, 2, 1], 0, 0, "len=0"),
        ([0, 0, 1, 0], 1, 4, "len=4 (0-3)"),
        ([2, 1, 3, 2, 1, 1], 5, 3, "len=3 (3-5)"),
        ([1, 2, 3], 10, 3, "len=3 (0-2)"),
        ([7, 8], 5, 0, "len=0"),
    ]
    w = [5, 22, 9, 22, 16, 16]
    print("Test results (variable window):")
    print(row(["No.", "Array", "Limit K", "Expected", "Brute force", "Sliding window", "Result"], w))
    for no, (a, limit, exp_len, exp_text) in enumerate(tests, start=1):
        b = longest_brute_force(a, limit)
        o = longest_sliding_window(a, limit)
        ok = valid_window(a, limit, b, exp_len) and valid_window(a, limit, o, exp_len)
        print(row([str(no), to_str(a), str(limit), exp_text, best_str(b), best_str(o),
                   "PASS" if ok else "FAIL"], w))
    print()
 
 
# =====================================================================
# Input validation checks
# =====================================================================
 
def run_validation_checks():
    checks = [
        ("A: a=[1, 2, 3], K=0", lambda: max_sum_sliding_window([1, 2, 3], 0)),
        ("A: a=[1, 2, 3], K=4 (K > n)", lambda: max_sum_sliding_window([1, 2, 3], 4)),
        ("A: a=[], K=1 (empty array)", lambda: max_sum_sliding_window([], 1)),
        ("B: a=[1, -2, 3], limit=4", lambda: longest_sliding_window([1, -2, 3], 4)),
        ("B: a=[1, 2], limit=-1", lambda: longest_sliding_window([1, 2], -1)),
    ]
    w = [5, 32, 12, 52]
    print("Validation checks:")
    print(row(["No.", "Case", "Expected", "Actual", "Result"], w))
    for no, (name, call) in enumerate(checks, start=1):
        rejected = False
        try:
            call()
            actual = "accepted"
        except ValueError as e:
            rejected = True
            actual = "rejected: " + str(e)
        print(row([str(no), name, "rejected", actual, "PASS" if rejected else "FAIL"], w))
    print()
 
 
if __name__ == "__main__":
    print("===== PART A: Maximum sum of K consecutive elements (fixed window) =====")
    run_fixed_tests()
    trace_fixed([2, 1, 5, 1, 3, 2], 3)
 
    print("===== PART B: Longest subarray with sum at most K (variable window) =====")
    run_variable_tests()
    trace_variable([1, 2, 1, 1, 3], 4)
    trace_variable([5, 1, 1], 2)
    trace_variable([4, 2, 1], 0)
 
    print("===== INPUT VALIDATION =====")
    run_validation_checks()
 