/*
 * Copyright 2015 Suprema(biostar2@suprema.co.kr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.supremainc.biostar2.sdk.models.v2.device;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

public class CardConfiguration implements Cloneable, Serializable {
    public static final String TAG = CardConfiguration.class.getSimpleName();
    private static final long serialVersionUID = 5883759635406887685L;
    @SerializedName("byte_order")
    int byte_order;
    @SerializedName("use_wiegand_format")
    boolean use_wiegand_format;
    @SerializedName("primary_key")
    String primary_key;
    @SerializedName("secondary_key")
    String secondary_key;
    @SerializedName("use_secondary_key")
    boolean use_secondary_key;
    @SerializedName("start_block_index")
    int start_block_index;
    @SerializedName("app_id")
    int app_id;
    @SerializedName("file_id")
    int file_id;
    @SerializedName("data_type")
    int data_type;
    @SerializedName("field_start")
    private ArrayList<Integer> field_start;
    @SerializedName("field_end")
    private ArrayList<Integer> field_end;

    @SuppressWarnings("unchecked")
    public CardConfiguration clone() throws CloneNotSupportedException {
        CardConfiguration target = (CardConfiguration) super.clone();
        if (field_start != null) {
            target.field_start = (ArrayList<Integer>) field_start.clone();
        }
        if (field_end != null) {
            target.field_end = (ArrayList<Integer>) field_end.clone();
        }
        return target;
    }
}