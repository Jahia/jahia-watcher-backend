# jahia-watcher-backend
This module contains all the backend functionality for the Jahia Watcher native mobile application

## Building

Checkout and build the jahia-spam-filtering module first, then build this project using : 

    mvn clean install

## Installing

In Jahia 7.2 it's as easy as connect to the SSH Shell and typing : 

    karaf> feature:repo-add mvn:org.jahia.modules/jahia-watcher-backend-karaf-feature/1.0-SNAPSHOT/xml/features
    karaf> feature:install jahia-watcher-backend-karaf-feature
    
Otherwise you will need to install first the jahia-spam-filtering bundle and then the jahia-watcher-backend module
either through the administration UI or using the mvn jahia:deploy goal.
