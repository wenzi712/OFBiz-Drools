/*
 * Copyright 2015 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uberfire.ext.metadata.backend.lucene.fields;

import java.util.Collection;
import java.util.Date;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.uberfire.ext.metadata.model.KProperty;
import org.uberfire.java.nio.base.version.VersionHistory;
import org.uberfire.java.nio.file.attribute.FileTime;

import static org.uberfire.ext.metadata.backend.lucene.util.Serializer.toByteArray;

public class SimpleFieldFactory implements FieldFactory {

    @Override
    public IndexableField[] build(final KProperty<?> property) {

        if (Enum.class.isAssignableFrom(property.getValue().getClass())) {
            if (property.isSearchable()) {
                return new IndexableField[]{new TextField(property.getName(),
                                                          property.getValue().toString().toLowerCase(),
                                                          Field.Store.YES)};
            }
            return new IndexableField[]{new StringField(property.getName(),
                                                        property.getValue().toString().toLowerCase(),
                                                        Field.Store.YES)};
        }

        if (property.getValue().getClass() == String.class) {
            if (property.isSortable()) {
                return new IndexableField[]{new SortedDocValuesField(property.getName(),
                                                                     new BytesRef(property.getValue().toString()))};
            }
            if (property.isSearchable()) {
                return new IndexableField[]{new TextField(property.getName(),
                                                          property.getValue().toString(),
                                                          Field.Store.YES)};
            }
            return new IndexableField[]{new StringField(property.getName(),
                                                        property.getValue().toString(),
                                                        Field.Store.YES)};
        }

        if (property.getValue().getClass() == Boolean.class) {
            if (property.isSearchable()) {
                return new IndexableField[]{new TextField(property.getName(),
                                                          ((Boolean) property.getValue()) ? "0" : "1",
                                                          Field.Store.YES)};
            }
            return new IndexableField[]{new StringField(property.getName(),
                                                        ((Boolean) property.getValue()) ? "0" : "1",
                                                        Field.Store.YES)};
        }

        if (property.getValue().getClass() == Integer.class) {
            if (property.isSearchable()) {
                return new IndexableField[]{new IntPoint(property.getName(),
                                                         (Integer) property.getValue())};
            }
            return new IndexableField[]{new StoredField(property.getName(),
                                                        (Integer) property.getValue())};
        }

        if (property.getValue().getClass() == Long.class) {
            if (property.isSearchable()) {
                return new IndexableField[]{new LongPoint(property.getName(),
                                                          (Long) property.getValue())};
            }
            return new IndexableField[]{new StoredField(property.getName(),
                                                        (Long) property.getValue())};
        }

        if (property.getValue().getClass() == Double.class) {
            if (property.isSearchable()) {
                return new IndexableField[]{new DoublePoint(property.getName(),
                                                            (Double) property.getValue())};
            }
            return new IndexableField[]{new StoredField(property.getName(),
                                                        (Double) property.getValue())};
        }

        if (property.getValue().getClass() == Float.class) {
            if (property.isSearchable()) {
                return new IndexableField[]{new FloatPoint(property.getName(),
                                                           (Float) property.getValue())};
            }
            return new IndexableField[]{new StoredField(property.getName(),
                                                        (Float) property.getValue())};
        }

        if (FileTime.class.isAssignableFrom(property.getValue().getClass())) {
            if (property.isSearchable()) {
                return new IndexableField[]{new LongPoint(property.getName(),
                                                          ((FileTime) property.getValue()).toMillis())};
            }
            return new IndexableField[]{new StoredField(property.getName(),
                                                        ((FileTime) property.getValue()).toMillis())};
        }

        if (Date.class.isAssignableFrom(property.getValue().getClass())) {
            if (property.isSearchable()) {
                return new IndexableField[]{new LongPoint(property.getName(),
                                                          ((Date) property.getValue()).getTime())};
            }
            return new IndexableField[]{new StoredField(property.getName(),
                                                        ((Date) property.getValue()).getTime())};
        }

        if (VersionHistory.class.isAssignableFrom(property.getValue().getClass())) {
            final VersionHistory versionHistory = (VersionHistory) property.getValue();
            return build(versionHistory);
        }

        if (Collection.class.isAssignableFrom(property.getValue().getClass())) {
            final StringBuilder sb = new StringBuilder();
            for (final java.lang.Object ovalue : (Collection) property.getValue()) {
                sb.append(ovalue).append(' ');
            }

            if (property.isSearchable()) {
                return new IndexableField[]{new TextField(property.getName(),
                                                          sb.toString(),
                                                          Field.Store.YES)};
            }
            return new IndexableField[]{new StringField(property.getName(),
                                                        sb.toString(),
                                                        Field.Store.YES)};
        }

        try {
            return new IndexableField[]{new StoredField(property.getName(),
                                                        toByteArray(property.getValue()))};
        } catch (final Exception ex) {
            return new IndexableField[]{new StoredField(property.getName(),
                                                        property.getValue().toString())};
        }
    }

    private IndexableField[] build(final VersionHistory versionHistory) {
        if (versionHistory.records().size() == 0) {
            return new IndexableField[]{};
        }

        final int lastIndex = versionHistory.records().size() - 1;
        final IndexableField checkinComment = build(new KProperty<String>() {
            @Override
            public String getName() {
                return "checkinComment";
            }

            @Override
            public String getValue() {
                return versionHistory.records().get(lastIndex).comment();
            }

            @Override
            public boolean isSearchable() {
                return true;
            }
        })[0];

        final IndexableField lastModifiedBy = build(new KProperty<String>() {
            @Override
            public String getName() {
                return "lastModifiedBy";
            }

            @Override
            public String getValue() {
                return versionHistory.records().get(lastIndex).author();
            }

            @Override
            public boolean isSearchable() {
                return true;
            }
        })[0];

        final IndexableField createdBy = build(new KProperty<String>() {
            @Override
            public String getName() {
                return "createdBy";
            }

            @Override
            public String getValue() {
                return versionHistory.records().get(0).author();
            }

            @Override
            public boolean isSearchable() {
                return true;
            }
        })[0];

        final IndexableField createdDate = build(new KProperty<Date>() {
            @Override
            public String getName() {
                return "createdDate";
            }

            @Override
            public Date getValue() {
                return versionHistory.records().get(0).date();
            }

            @Override
            public boolean isSearchable() {
                return true;
            }
        })[0];

        final IndexableField lastModifiedDate = build(new KProperty<Date>() {
            @Override
            public String getName() {
                return "lastModifiedDate";
            }

            @Override
            public Date getValue() {
                return versionHistory.records().get(lastIndex).date();
            }

            @Override
            public boolean isSearchable() {
                return true;
            }
        })[0];

        return new IndexableField[]{checkinComment, lastModifiedBy, createdBy, createdDate, lastModifiedDate};
    }
}
