# Assignment 2  
## 1DV701

Author: Christoffer Eid  
Author: Olof Enström  
Semester: Spring 2022  
Email: ce223af@student.lnu.se  
Email: oe222fh@student.lnu.se

---
## Table of contents
- [Assignment 2](#assignment-2)
  - [1DV701](#1dv701)
  - [Table of contents](#table-of-contents)
  - [Problem 1](#problem-1)
    - [Exceptions](#exceptions)
      - [FileNotFoundException](#filenotfoundexception)
      - [IllegalArgumentException](#illegalargumentexception)
      - [SocketException](#socketexception)
      - [IOException](#ioexception)
  - [Problem 2](#problem-2)
    - [302 Redirect](#302-redirect)
    - [404 Not Found](#404-not-found)
    - [500 Internal Server Error](#500-internal-server-error)
  - [VG](#vg)

## Problem 1
![Named HTML page](img/named-html-page.png)  
awdawd

![Image support](img/image.png)  
awdawd

![Directory](img/directory-with-index.png)  

### Exceptions

#### FileNotFoundException

#### IllegalArgumentException

#### SocketException



#### IOException

If an input or output error occurs during creation of the output socket stream or if the socket is not connected.

If an input or output error occurs during closing of streams and sockets, an `IOException` will occur.

## Problem 2
### 302 Redirect
![302 Redirect](img/redirect.png)  
Navigating to the URL `/redirect.html` the 302 response code will be triggered and you are redirected to the `index.html` page in the root folder.

### 404 Not Found
![404 Not Found](img/not-found.png)  

Make a request to any file or directory that does not exist in the `public` folder and the 404 response will be triggered.

### 500 Internal Server Error
![500 Internal Server Error](img/internal-server-error.png)  
Make any request that is not GET or POST and the 500 Internal Server Error will be triggered.

## VG
For the image parsing we are looking for the specific start bytes of a PNG file or JPG file. This indicates the start of bytes to write to the image. We then do the same for the end bytes of PNG and JPG. To look for the start or end sequence we use the Knuth-Morris-Pratt algorithm for pattern matching.