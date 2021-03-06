// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.lang.html.psi.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.impl.source.xml.XmlAttributeImpl;
import com.intellij.psi.xml.XmlElement;
import org.angular2.lang.html.parser.Angular2AttributeNameParser;
import org.angular2.lang.html.parser.Angular2AttributeNameParser.AttributeInfo;
import org.angular2.lang.html.parser.Angular2HtmlElementTypes.Angular2ElementType;
import org.angular2.lang.html.psi.Angular2HtmlBoundAttribute;
import org.jetbrains.annotations.NotNull;

import static com.intellij.psi.xml.XmlTokenType.XML_NAME;

public class Angular2HtmlBoundAttributeImpl extends XmlAttributeImpl implements Angular2HtmlBoundAttribute {

  private static final Logger LOG = Logger.getInstance(Angular2HtmlBoundAttributeImpl.class);

  public Angular2HtmlBoundAttributeImpl(@NotNull Angular2ElementType elementType) {
    super(elementType);
  }

  @Override
  public XmlElement getNameElement() {
    XmlElement result = super.getNameElement();
    if (result == null && getFirstChild() instanceof PsiErrorElement
        && getFirstChild().getFirstChild().getNode().getElementType() == XML_NAME) {
      return (XmlElement)getFirstChild().getFirstChild();
    }
    return result;
  }

  protected AttributeInfo getAttributeInfo() {
    AttributeInfo info = Angular2AttributeNameParser.parseBound(getName());
    if(info.elementType != getElementType()) {
      LOG.error("Element type mismatch on attribute info. Expected " + getElementType()
                + ", but got " + info.elementType +". Error for " + toString(), new Throwable());
    }
    return info;
  }

  @Override
  public String toString() {
    return StringUtil.trimEnd(getClass().getSimpleName(), "Impl") + " " + getAttributeInfo().toString();
  }
}
