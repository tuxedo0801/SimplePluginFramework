/**
 * This file is part of "Simple Plugin Framework".
 * 
 *  "Simple Plugin Framework" is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  "Simple Plugin Framework" is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with "Simple Plugin Framework".  
 *  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  (c) 2016, Alexander Christian <info@root1.de>
 */
package de.root1.spf;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 *
 * @author ACHR
 */
public class FileList {
    
    private List<FileItem> fileItemList = new ArrayList<FileItem>();

    class FileItem {

        private String filename;
        private long size;
        private long lastModified;

        public FileItem(File f) {
           lastModified = f.lastModified();
           filename = f.getName();
           size = f.length();
        }

        @Override
        public boolean equals(Object obj) {

            // other types are not compareable
            if (!(obj instanceof FileItem))
                return false;

            FileItem other = (FileItem) obj;

            // only return true, if all fields are equal
            if (this.filename.equals(other.filename) &&
                    this.size==other.size &&
                    this.lastModified==other.lastModified)
                return true;

            // in all other cases, both FileItems are not equal
            return false;
        }



    }

    public FileList(File[] fileList) {

        Arrays.sort(fileList);
        
        for(File file : fileList) {

            if (file.isFile()) {
                FileItem fi = new FileItem(file);
                fileItemList.add(fi);
            }
        }


    }
    
    private int getSize(){
        return fileItemList.size();
    }

    private FileItem getIndex(int i){
        return fileItemList.get(i);
    }

    @Override
    public boolean equals(Object obj) {

        // other types are not compareable
        if (!(obj instanceof FileList))
            return false;

        FileList other = (FileList) obj;

        // if the other list has different size, they are not equal
        if (this.getSize()!=other.getSize())
            return false;

        // if at least one item in the other list doesnt match this list, they are not equal
        for (int i=0; i<this.getSize(); i++) {
            if (!this.getIndex(i).equals(other.getIndex(i)))
                return false;
        }

        // in all other cases, both lists are equal
        return true;
    }





}
