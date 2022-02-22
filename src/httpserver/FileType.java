package httpserver;

import java.util.Arrays;
import java.util.List;

enum FileType {
  PNG(Arrays.asList(137, 80, 78, 71, 13, 10, 26, 10),
      Arrays.asList(73, 69, 78, 68, 174, 66, 96, 130)),
  JPG(Arrays.asList(255, 216, 255), Arrays.asList(255, 217, 13, 10));

  public List<Integer> start;
  public List<Integer> end;

  FileType(List<Integer> start, List<Integer> end) {
    this.start = start;
    this.end = end;
  }
}