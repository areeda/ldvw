#include "ParserInstruction.h"

int ParserInstruction::OpenNode = 1;
int ParserInstruction::CloseNode = 2;
int ParserInstruction::WriteAttributeKey = 3;
int ParserInstruction::WriteAttributeValue = 4;
int ParserInstruction::AddText = 5;
int ParserInstruction::AddContent = 6;
int ParserInstruction::CloseLeaf = 7;
int ParserInstruction::AddLeaf = 8;
int ParserInstruction::AddEntity = 9;
int ParserInstruction::AddComment = 10;
int ParserInstruction::SetTitle = 11;
int ParserInstruction::AddProcessingInstruction = 12;
int ParserInstruction::AddDoctypeDecl = 13;
