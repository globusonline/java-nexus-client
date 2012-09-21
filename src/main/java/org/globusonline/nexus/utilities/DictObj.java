package org.globusonline.nexus.utilities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
public class DictObj {
//    Simple dictionary wrapper

	private Map<String, String> delegate;
	
    private void init(String delegate){
        this.delegate = new HashMap<String, String>();
    }

    public int len(){
        return delegate.size();
    }
    
//    public  Iterator<?> iter(){
//        return this.delegate.__iter__()
//    }

    public String getItem(String item){
        return this.delegate.get(item);
    }

//    def __getattr__(self, attrname):
//        try:
//            return self.delegate[attrname]
//        except KeyError:
//            raise AttributeError()
	
}
