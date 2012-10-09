Globus-Nexus-Java-Client
========================

Java Client for interacting with Globus Nexus

/*
Copyright 2012 Johns Hopkins University Institute for Computational Medicine
Based upon the GlobusOnline Nexus Client written in Python by Mattias Lidman  
available at https://github.com/globusonline/python-nexus-client

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
/**
* @author Chris Jurado
* 
*/

Most of the files in the client are directly converted from the Python code from the original client.  
An additional class, NexusClient, was added to abstract the JSON handling and provide methods to quickly and easily 
perform simple operations on the data.

This project is a work in progress and should be considered Beta.  You can report bugs to jbryan@ci.uchicago.edu, 
or if you are willing to fix them yourself please issue a pull request.

Additionally, a nexus.config file has been added to allow configuration of the Nexus default community and url.