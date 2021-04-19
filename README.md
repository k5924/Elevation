# Elevation

## Introduction

> We are provided with a working application that searches a raster of elevation data for regions of relative flatness. The raster is an image in which each pixel represents an elevation for a point on the terrain. The application can be imagined to be searching for potential landing sites for an aircraft or spacecraft of some kind.
rgbelevation contains example images to use with the program

## SearchUIEnhancement
>
<ol>
<li>Run the search on a worker thread so the UI doesnt freeze</li>
<li>Display output from the searcher in the text area under the raster</li>
<li>A user can cancel the operation and quit the program by closing the window</li>
<li>A user can cancel the operation without quitting the program through a cancel button</li>
<li>A progress bar is updated each second by a dedicated thread that monitors the progress of the search</li>
</ol>

## Parallelisation
>
<ol>
<li>Make a class that extends recursive action which searches the raster for points of elevation</li>
<li>Implement a compute method to make new threads in the fork join pool</li>
<li>Discard information and update messages from the search listener</li>
</ol>

## Considerations
> All code must implement thread-safety correctly

## Languages and Tools:
>
<p align="left"> <a href="https://www.java.com" target="_blank"> <img src="https://devicons.github.io/devicon/devicon.git/icons/java/java-original-wordmark.svg" alt="java" width="40" height="40"/> </a> </p>
