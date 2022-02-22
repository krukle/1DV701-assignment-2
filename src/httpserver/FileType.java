package httpserver;

import java.util.Arrays;
import java.util.List;

enum FileType {
  PNG(Arrays.asList(137, 80, 78, 71, 13, 10, 26, 10),
      Arrays.asList(73, 69, 78, 68, 174, 66, 96, 130), 
      Arrays.asList("png")),
  JPG(Arrays.asList(255, 216, 255), 
      Arrays.asList(255, 217, 13, 10), 
      Arrays.asList("jpg", "jpeg", "jpe", "jif", "jfif", "jfi"));

  public List<Integer> start;
  public List<Integer> end;
  public List<String> extensions;

  FileType(List<Integer> start, List<Integer> end, List<String> extensions) {
    this.start     = start;
    this.end       = end;
    this.extensions = extensions;
  }

  /**
   * Iterates all FileType extensions until match is found with string. 
   *
   * @param string String representation of file extension.
   * @return FileType if match is found, else throws IllegalArgumentException.
   */
  public static FileType fromString(String string) {
    for (FileType fileType : FileType.values()) {
      for (String extension : fileType.extensions) {
        if (extension.equalsIgnoreCase(string)  || extension.equalsIgnoreCase("." + string)) {
          return fileType;
        }
      }
    }
    throw new IllegalArgumentException("No FileType with file ending " + string + " found.");
  }
}