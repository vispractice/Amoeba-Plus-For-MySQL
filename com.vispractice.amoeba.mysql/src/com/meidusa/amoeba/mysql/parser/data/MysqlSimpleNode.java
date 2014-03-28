/*
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.amoeba.mysql.parser.data;

import com.meidusa.amoeba.mysql.parser.sql.MysqlParserConstants;
import com.meidusa.amoeba.mysql.parser.sql.MysqlParserTreeConstants;
import com.meidusa.amoeba.mysql.parser.sql.Token;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public abstract class MysqlSimpleNode {
	  protected Token firstToken;
	  protected Token lastToken;
	  protected String nodeName;
	  public Comparable<?> value;
	  public MysqlSimpleNode(int id){
		  nodeName = MysqlParserTreeConstants.jjtNodeName[id];
	  }
	  
	  public Token getFirstToken(){
		  return firstToken;
	  }

	  public Token getLastToken(){
		  return lastToken;
	  }

	  public void setFirstToken(Token token){
		  firstToken = token;
	  }

	  public void setLastToken(Token token){
		  lastToken = token;
	  }
	  
	  public Object getNodeName() {
			return nodeName;
	 }
	 
	  public String getNodeValue(){
		  return formatTokens(firstToken, lastToken);
	  }
	  
	  private String formatTokens(Token first, Token last){
			StringBuffer sb = new StringBuffer();
		    Token t = first;
		    int endColumn = last.endColumn;
		    int endLine = last.endLine;
		    while(t != null && (t.endLine < endLine || (t.endLine == endLine && t.endColumn <= endColumn))){
		    	sb.append(formatToken(t));
		  		t = t.next;
		    }
		    return sb.toString();
		}	
		
	  
	  private String formatToken(Token token){
			String image = token.image;
			if(image.equals(".")){
				return image;
			}		

			StringBuffer sb = new StringBuffer();
			if(token.kind == MysqlParserConstants.IDENTIFIER ||
			   token.kind == MysqlParserConstants.S_QUOTED_IDENTIFIER
					){
				//Table Name or Column Name.. Change it to lower case
				sb.append(image.toLowerCase());
			}else if (token.kind == MysqlParserConstants.STRING_LITERAL){
				//This is a constant in SQL.. Leave it as its
				sb.append(image);
			}else{
				//All other are keywords so make them upper case.. Or chars that are case insestive
				sb.append(image.toUpperCase());
			}

			Token next = token.next;
			if(next != null){
				String nextImage = next.image;
				if(!(nextImage.startsWith(".") || nextImage.equals(","))){
					sb.append(" ");
				}
			}		
			
			return sb.toString();
		}	
}

