import re
import sys


def rearrange(filename):
    with open(filename, 'r') as file:
        content = file.read()

    # Regex to find the blocks of code to rearrange
    pattern = re.compile(r'(putIndex\(new SimpleSubscriberInfo\(.*?\)\);)', re.DOTALL)
    blocks = pattern.findall(content)

    # Sort blocks based on the class names mentioned in SimpleSubscriberInfo instances
    sorted_blocks = sorted(blocks, key=lambda x: re.search(r'SimpleSubscriberInfo\((.*?),', x).group(1))

    # Replace the original blocks with the sorted blocks
    sorted_content = pattern.sub(lambda match: sorted_blocks.pop(0), content)

    with open(filename, 'w') as file:
        file.write(sorted_content)


# Project root relative to the script
project_root = __file__[:-len('/scripts/fixEventBus.py')]

path = './build/generated/ap_generated_sources/release/out/eu/basicairdata/graziano/gpslogger/EventBusIndex.java'

# Print the path to the file to stderr
print(path, file=sys.stderr)

# Call the function with the path to EventBusIndex.java
rearrange(path)
