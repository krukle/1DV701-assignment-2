package httpserver;

enum FileType {
  PNG(new byte[]{-119, 80, 78, 71, 13, 10, 26, 10},
      new byte[]{73, 69, 78, 68, -82, 66, 96, -126}, 
      new String[]{"png"}),
  JPG(new byte[]{-1, -40, -1}, 
      new byte[]{-1, -39, 13, 10}, 
      new String[]{"jpg", "jpeg", "jpe", "jif", "jfif", "jfi"});

  public byte[] start;
  public byte[] end;
  public String[] extensions;

  FileType(byte[] start, byte[] end, String[] extensions) {
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