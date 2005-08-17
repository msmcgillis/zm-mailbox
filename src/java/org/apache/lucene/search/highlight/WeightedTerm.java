package org.apache.lucene.search.highlight;
/**
 * Copyright 2002-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Lightweight class to hold term and a weight value used for scoring this term 
 * @author Mark Harwood
 */
public class WeightedTerm
{
	float weight; // multiplier
	String term; //stemmed form
	public WeightedTerm (float weight,String term)
	{
		this.weight=weight;
		this.term=term;
	}
	
	
	/**
	 * @return the term value (stemmed)
	 */
	public String getTerm()
	{
		return term;
	}

	/**
	 * @return the weight associated with this term
	 */
	public float getWeight()
	{
		return weight;
	}

	/**
	 * @param term the term value (stemmed)
	 */
	public void setTerm(String term)
	{
		this.term = term;
	}

	/**
	 * @param weight the weight associated with this term
	 */
	public void setWeight(float weight)
	{
		this.weight = weight;
	}

}
