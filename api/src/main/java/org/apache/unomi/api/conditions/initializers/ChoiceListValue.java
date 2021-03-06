/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.unomi.api.conditions.initializers;

/**
 * List option object for various choice lists.
 */
public class ChoiceListValue implements Cloneable {

    private String id;

    private String name;

    public ChoiceListValue(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns a cloned instance of this choice list value object with the name, set to the provided localized name.
     * 
     * @param localizedName
     *            the localized name for this choice list value object
     * @return a cloned instance of this choice list value object with the name, set to the provided localized name
     */
    public ChoiceListValue localizedCopy(String localizedName) {
        try {
            ChoiceListValue clone = (ChoiceListValue) clone();
            clone.name = localizedName;

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException(e);
        }

    }
}
