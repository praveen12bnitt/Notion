// Default anonymization script

// Use AccessionNumber if available, StudyInstanceUID if not.
// Prevents creation of extra AccessionNumbers
var aLookup = tags.AccessionNumber || tags.StudyInstanceUID;

// Zero pad a number to 4 digits
function pad (s) {
  var n = new java.lang.Integer ( s )
  // Zero pad
  return java.lang.String.format ( "%04d", n )
}

// The anonymizer expects a hash to be the last statement in this script.
// Any fields in the hash that map to DICOM tags will replace the tags in the
// saved DICOM.  The 'tags' hash contains the original tags for this DICOM file.
// Please see Notion's documentation for more details.

var tags = {
   PatientName: anonymizer.lookup('PatientName', tags.PatientName ) || 'PN-' + pad(anonymizer.sequenceNumber ( 'PatientName', tags.PatientName)),
   PatientID: anonymizer.lookup('PatientID', tags.PatientID ) || anonymizer.sequenceNumber ( 'PatientID', tags.PatientID),
   AccessionNumber: anonymizer.lookup('AccessionNumber', aLookup ) || anonymizer.sequenceNumber ( 'AccessionNumber', aLookup),
 };
tags;
