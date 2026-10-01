import re
import sys
import xml.etree.ElementTree as ET

ASSERTION_TYPES = ("org.opentest4j.AssertionFailedError",)
ASSERTION_SUFFIX = "ComparisonFailure"
MULTI_CAUSE_TYPE = "org.gradle.internal.exceptions.DefaultMultiCauseException"


def is_assertion_type(failure_type):
    return failure_type in ASSERTION_TYPES or failure_type.endswith(ASSERTION_SUFFIX)


def is_assertion_failure(node):
    failure_type = node.get("type", "")
    if is_assertion_type(failure_type):
        return True
    if failure_type != MULTI_CAUSE_TYPE:
        return False

    # Gradle wraps multiple failures from one test in DefaultMultiCauseException.
    # Its message lists each direct cause on a tab-indented line before the trace.
    causes = re.findall(r"^\t([^:\s]+):", node.get("message", ""), re.MULTILINE)
    return bool(causes) and all(is_assertion_type(cause) for cause in causes)

total = assertion_failed = other_failed = skipped = unreadable = 0

# Counted from the <testcase> elements rather than the <testsuite> attributes:
# a suite that died early still carries a count that its cases do not back.
for path in sys.argv[1:]:
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        unreadable += 1
        continue
    for testcase in root.iter("testcase"):
        total += 1
        node = testcase.find("failure")
        if node is None:
            node = testcase.find("error")
        if node is None:
            if testcase.find("skipped") is not None:
                skipped += 1
            continue
        if is_assertion_failure(node):
            assertion_failed += 1
        else:
            other_failed += 1

print(f"{total} {assertion_failed} {other_failed} {skipped} {unreadable}")
