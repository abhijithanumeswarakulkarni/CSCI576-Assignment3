# CSCI576-Assignment3
## Assignment 3 - Multimedia Design
Author: Abhijit Hanumeswara Kulkarni
USC ID: 5367018274

### Steps to run the project (DCT vs DWT decoding)
1) Navigate to the src directory
2) Compile all the main packages, using below command:
> javac main/*.java
3) Run the main.Assignment file to get the results with input parameters. Sample command as below:
> java main.Assignment "./img/yosemite_firefall.rgb" 65536 
4) This should open a window popup with DCT and DWT decoding side by side decoded using the coefficients provided.
6) Close the window to terminate the program

## Steps to run the project (Progressive DCT vs DWT decoding) Pat A:
1) Navigate to the src directory
2) Compile all the main packages, using below command:
> javac main/*.java
3) Run the main.Assignment file to get the results with input parameters. Sample command as below:
> java main.Assignment "./img/yosemite_firefall.rgb" -1
4) This should open two window popups with DCT and DWT decoding respectively. First window is DCT Progressive decoding while the second window is DWT progressive decoding.
5) Also you could see the iteration step in the console
6) Close the window to terminate the program

## Steps to run the project (Progressive DCT and DWT decoding) Pat B:
1) Navigate to the src directory
2) Compile all the main packages, using below command:
> javac main/*.java
3) Run the main.Assignment file to get the results with input parameters. Sample command as below:
> java main.Assignment "./img/yosemite_firefall.rgb" -2
4) This should open a window popup with DCT and DWT decoding (Picking the best between both)
5) Also you could see the iteration step in the console
6) Close the window to terminate the program