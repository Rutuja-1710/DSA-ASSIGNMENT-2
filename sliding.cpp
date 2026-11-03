
#include <algorithm>
#include <climits>
#include <functional>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>
using namespace std;
 
// ---------- formatting helpers ----------
string pad(const string& s, size_t w) { return s.size() >= w ? s : s + string(w - s.size(), ' '); }
 
string row(const vector<string>& cells, const vector<size_t>& w) {
    string r;
    for (size_t i = 0; i < cells.size(); i++)
        r += (i + 1 == cells.size()) ? cells[i] : pad(cells[i], w[i]);
    return r;
}
 
string toStr(const vector<int>& v) {
    string s = "[";
    for (size_t i = 0; i < v.size(); i++) {
        if (i) s += ", ";
        s += to_string(v[i]);
    }
    return s + "]";
}
 
string rangeStr(int l, int r) { return "[" + to_string(l) + ".." + to_string(r) + "]"; }
 
// elements a[l..r] written as [2,1,5]
string elems(const vector<int>& a, int l, int r) {
    string s = "[";
    for (int i = l; i <= r; i++) {
        if (i > l) s += ",";
        s += to_string(a[i]);
    }
    return s + "]";
}
 
// elements a[l..r] written as [1, 2, 1]
string sliceStr(const vector<int>& a, int l, int r) {
    string s = "[";
    for (int i = l; i <= r; i++) {
        if (i > l) s += ", ";
        s += to_string(a[i]);
    }
    return s + "]";
}
 
// =====================================================================
// PART A: Maximum sum of a subarray of size K (fixed window)
// =====================================================================
 
void validateFixed(const vector<int>& a, int k) {
    if (k < 1 || k > (int)a.size()) throw invalid_argument("K must satisfy 1 <= K <= n");
}
 
// Reference: recompute every K-element sum from scratch.
// Time O(n*K), auxiliary space O(1).
long long maxSumReference(const vector<int>& a, int k) {
    validateFixed(a, k);
    int n = (int)a.size();
    long long best = LLONG_MIN;
    for (int start = 0; start + k <= n; start++) {
        long long sum = 0;
        for (int j = start; j < start + k; j++) sum += a[j];   // K additions every time
        best = max(best, sum);
    }
    return best;
}
 
// Optimized: compute the first window once, then slide:
//   sum = sum + entering element - leaving element.
// Time O(n), auxiliary space O(1).
long long maxSumSlidingWindow(const vector<int>& a, int k) {
    validateFixed(a, k);
    int n = (int)a.size();
    long long sum = 0;
    for (int i = 0; i < k; i++) sum += a[i];                   // first window, computed once
    long long best = sum;
    for (int i = k; i < n; i++) {
        sum += a[i];                                           // entering element a[i]
        sum -= a[i - k];                                       // leaving element a[i-K]
        best = max(best, sum);
    }
    return best;
}
 
void traceFixed(const vector<int>& a, int k) {
    cout << "Dry run (fixed window) on " << toStr(a) << ", K = " << k << "\n";
    vector<size_t> w = {6, 10, 13, 10, 10, 7};
    cout << row({"Step", "Window", "Elements", "Enter(+)", "Leave(-)", "Sum", "Max so far"}, w) << "\n";
    int n = (int)a.size();
    long long sum = 0;
    for (int i = 0; i < k; i++) sum += a[i];
    long long best = sum;
    cout << row({"init", rangeStr(0, k - 1), elems(a, 0, k - 1), "-", "-", to_string(sum), to_string(best)}, w) << "\n";
    for (int i = k; i < n; i++) {
        int enter = a[i], leave = a[i - k];
        sum += enter;
        sum -= leave;
        best = max(best, sum);
        cout << row({to_string(i - k + 1), rangeStr(i - k + 1, i), elems(a, i - k + 1, i),
                     to_string(enter), to_string(leave), to_string(sum), to_string(best)}, w) << "\n";
    }
    cout << "  Result: maximum sum = " << best << "\n\n";
}
 
struct FixedTest { vector<int> a; int k; long long expected; };
 
void runFixedTests() {
    vector<FixedTest> tests = {
        {{2, 1, 5, 1, 3, 2}, 3, 9},
        {{-4, -2, -7}, 2, -6},
        {{6}, 1, 6},
        {{1, 2, 3}, 3, 6},
        {{3, -1, 4}, 1, 4},
        {{-2, 3, -1, 2}, 2, 2},
    };
    vector<size_t> w = {5, 22, 5, 10, 11, 16};
    cout << "Test results (fixed window):\n";
    cout << row({"No.", "Array", "K", "Expected", "Reference", "Sliding window", "Result"}, w) << "\n";
    int no = 0;
    for (auto& t : tests) {
        no++;
        long long ref = maxSumReference(t.a, t.k);
        long long opt = maxSumSlidingWindow(t.a, t.k);
        bool ok = (ref == t.expected && opt == t.expected);
        cout << row({to_string(no), toStr(t.a), to_string(t.k), to_string(t.expected),
                     to_string(ref), to_string(opt), ok ? "PASS" : "FAIL"}, w) << "\n";
    }
    cout << "\n";
}
 
// =====================================================================
// PART B: Longest subarray with sum <= K (variable window)
// =====================================================================
 
struct WinResult { int length; int start; int end; };   // length 0 -> start = end = -1
 
void validateVariable(const vector<int>& a, int limit) {
    if (limit < 0) throw invalid_argument("limit K must be non-negative");
    for (int x : a)
        if (x < 0) throw invalid_argument("array values must be non-negative");
}
 
string bestStr(const WinResult& b) {
    if (b.length == 0) return "len=0";
    return "len=" + to_string(b.length) + " " + rangeStr(b.start, b.end);
}
 
// Straightforward: for every start index, extend the end while the sum stays <= K.
// Because values are non-negative the sum never decreases when extending, so we
// can stop as soon as it exceeds K.  Time O(n^2), auxiliary space O(1).
WinResult longestBruteForce(const vector<int>& a, int limit) {
    validateVariable(a, limit);
    int n = (int)a.size();
    WinResult best{0, -1, -1};
    for (int s = 0; s < n; s++) {
        long long sum = 0;
        for (int e = s; e < n; e++) {
            sum += a[e];
            if (sum > limit) break;
            int len = e - s + 1;
            if (len > best.length) best = {len, s, e};
        }
    }
    return best;
}
 
// Optimized: expand right, contract left while sum > K.
// Each element enters once and leaves at most once -> O(n) time, O(1) space.
WinResult longestSlidingWindow(const vector<int>& a, int limit) {
    validateVariable(a, limit);
    int n = (int)a.size();
    WinResult best{0, -1, -1};
    long long sum = 0;
    int left = 0;
    for (int right = 0; right < n; right++) {
        sum += a[right];                                   // expand: add new right element
        while (sum > limit && left <= right) {             // contract: window too heavy
            sum -= a[left];
            left++;
        }
        int len = right - left + 1;                        // window is valid here (or empty)
        if (len > best.length) best = {len, left, right};  // update best
    }
    return best;
}
 
void traceVariable(const vector<int>& a, int limit) {
    cout << "Dry run (variable window) on " << toStr(a) << ", K = " << limit << "\n";
    vector<size_t> w = {6, 7, 6, 40, 6, 6};
    cout << row({"Step", "Right", "Left", "Action", "Sum", "Len", "Best"}, w) << "\n";
    int n = (int)a.size();
    WinResult best{0, -1, -1};
    long long sum = 0;
    int left = 0, step = 0;
    for (int right = 0; right < n; right++) {
        sum += a[right];
        step++;
        cout << row({to_string(step), to_string(right), to_string(left),
                     "expand: add a[" + to_string(right) + "]=" + to_string(a[right]),
                     to_string(sum), to_string(right - left + 1), bestStr(best)}, w) << "\n";
        while (sum > limit && left <= right) {
            int removed = a[left];
            sum -= removed;
            left++;
            step++;
            cout << row({to_string(step), to_string(right), to_string(left),
                         "contract: sum > K, remove a[" + to_string(left - 1) + "]=" + to_string(removed),
                         to_string(sum), to_string(right - left + 1), bestStr(best)}, w) << "\n";
        }
        int len = right - left + 1;
        string action;
        if (len > best.length) {
            action = "valid: len " + to_string(len) + " > best " + to_string(best.length) + ", update best";
            best = {len, left, right};
        } else {
            action = "valid: len " + to_string(len) + " <= best " + to_string(best.length) + ", keep best";
        }
        step++;
        cout << row({to_string(step), to_string(right), to_string(left), action,
                     to_string(sum), to_string(len), bestStr(best)}, w) << "\n";
    }
    if (best.length == 0)
        cout << "  Result: longest length = 0 (no valid subarray)\n\n";
    else
        cout << "  Result: longest length = " << best.length << ", subarray " << sliceStr(a, best.start, best.end)
             << " (indices " << best.start << ".." << best.end << ")\n\n";
}
 
struct VarTest { vector<int> a; int limit; int expectedLen; string expectedText; };
 
bool validWindow(const vector<int>& a, int limit, const WinResult& r, int expectedLen) {
    if (r.length != expectedLen) return false;
    if (r.length == 0) return r.start == -1 && r.end == -1;
    if (r.start < 0 || r.end >= (int)a.size() || r.end - r.start + 1 != r.length) return false;
    long long sum = 0;
    for (int i = r.start; i <= r.end; i++) sum += a[i];
    return sum <= limit;
}
 
void runVariableTests() {
    vector<VarTest> tests = {
        {{1, 2, 1, 1, 3}, 4, 3, "len=3 (0-2 or 1-3)"},
        {{5, 1, 1}, 2, 2, "len=2 (1-2)"},
        {{4, 2, 1}, 0, 0, "len=0"},
        {{0, 0, 1, 0}, 1, 4, "len=4 (0-3)"},
        {{2, 1, 3, 2, 1, 1}, 5, 3, "len=3 (3-5)"},
        {{1, 2, 3}, 10, 3, "len=3 (0-2)"},
        {{7, 8}, 5, 0, "len=0"},
    };
    vector<size_t> w = {5, 22, 9, 22, 16, 16};
    cout << "Test results (variable window):\n";
    cout << row({"No.", "Array", "Limit K", "Expected", "Brute force", "Sliding window", "Result"}, w) << "\n";
    int no = 0;
    for (auto& t : tests) {
        no++;
        WinResult b = longestBruteForce(t.a, t.limit);
        WinResult o = longestSlidingWindow(t.a, t.limit);
        bool ok = validWindow(t.a, t.limit, b, t.expectedLen) && validWindow(t.a, t.limit, o, t.expectedLen);
        cout << row({to_string(no), toStr(t.a), to_string(t.limit), t.expectedText,
                     bestStr(b), bestStr(o), ok ? "PASS" : "FAIL"}, w) << "\n";
    }
    cout << "\n";
}
 
// =====================================================================
// Input validation checks
// =====================================================================
 
void runValidationChecks() {
    struct Check { string name; function<void()> call; };
    vector<Check> checks = {
        {"A: a=[1, 2, 3], K=0", [] { maxSumSlidingWindow({1, 2, 3}, 0); }},
        {"A: a=[1, 2, 3], K=4 (K > n)", [] { maxSumSlidingWindow({1, 2, 3}, 4); }},
        {"A: a=[], K=1 (empty array)", [] { maxSumSlidingWindow({}, 1); }},
        {"B: a=[1, -2, 3], limit=4", [] { longestSlidingWindow({1, -2, 3}, 4); }},
        {"B: a=[1, 2], limit=-1", [] { longestSlidingWindow({1, 2}, -1); }},
    };
    vector<size_t> w = {5, 32, 12, 52};
    cout << "Validation checks:\n";
    cout << row({"No.", "Case", "Expected", "Actual", "Result"}, w) << "\n";
    int no = 0;
    for (auto& c : checks) {
        no++;
        string actual;
        bool rejected = false;
        try { c.call(); actual = "accepted"; }
        catch (const invalid_argument& e) { rejected = true; actual = string("rejected: ") + e.what(); }
        cout << row({to_string(no), c.name, "rejected", actual, rejected ? "PASS" : "FAIL"}, w) << "\n";
    }
    cout << "\n";
}
 
int main() {
    cout << "===== PART A: Maximum sum of K consecutive elements (fixed window) =====\n";
    runFixedTests();
    traceFixed({2, 1, 5, 1, 3, 2}, 3);
 
    cout << "===== PART B: Longest subarray with sum at most K (variable window) =====\n";
    runVariableTests();
    traceVariable({1, 2, 1, 1, 3}, 4);
    traceVariable({5, 1, 1}, 2);
    traceVariable({4, 2, 1}, 0);
 
    cout << "===== INPUT VALIDATION =====\n";
    runValidationChecks();
    return 0;
}
 