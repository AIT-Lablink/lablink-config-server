//
// Copyright (c) AIT Austrian Institute of Technology GmbH.
// Distributed under the terms of the Modified BSD License.
//

package at.ac.ait.lablink.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class ConfigTest {

  @Test
  public void testTest() {
    assertEquals("AIT Lablink Configuration Server", Utility.INFO_PRODUCT);
  }

  @Test
  public void testTokenSplit() {
    assertTrue(Config.splitLen("python:DPBLablink:getJson") == 3,
        "Length should be 3 when three tokens are passed.");
  }

  @Test
  public void testValidateInvokeCheckLengthWithPython() {
    assertTrue(Config.validateInvoke("python:DPBLablink:getJson"),
        "Validation failed for the invoke token.");
  }

  @Test
  public void testValidateInvokeCheckOtherLanguageToken() {
    assertFalse(Config.validateInvoke("java:DPBLablink:getJson"),
        "The language token must be 'python'.");
  }

  @Test
  public void testValidateInvokeCheckLength() {
    assertFalse(Config.validateInvoke("python:DPBLablink:getJson:error"),
        "Should fail when provided with more than 3 tokens.");
  }

  @Test
  public void testValidateInvokeCheckClassTokenBlank() {
    assertTrue(Config.validateInvoke("python::getJson"),
        "Should pass when class name token token is blank.");
  }

  @Test
  public void testValidateInvokeCheckMethodTokenBlank() {
    assertFalse(Config.validateInvoke("python:getJson:"),
        "Should fail when method name token is blank.");
  }

  @Test
  public void testValidateInvokeLanguageTokenBlank() {
    assertFalse(Config.validateInvoke(":DPBLablink:getJson"),
        "Should fail when languge token is blank.");
  }

  @DisplayName("Test the behaviour if a null token is passed...")
  @Test
  public void testValidateInvokeTokenNull() {
    assertFalse(Config.validateInvoke(null), "Should fail token is null.");
  }

  @DisplayName("Test if the logo.png exists...")
  @Test
  public void testIfTheLogoExistsRelative() {
    assertFalse(getClass().getResource("/logo.png").getPath() == null,
        "Should fail if the logo.png is missing.");
  }

  // @Test
  // public void testIfTheLogoExistsRoot() {
  // assertFalse(getClass().getResource("/resources/logo.png") == null,
  // "Should fail if the logo.png is missing.");
  // }

}
