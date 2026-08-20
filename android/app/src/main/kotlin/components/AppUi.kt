package com.seipseip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable internal fun Page(title:String,back:()->Unit,content:@Composable ColumnScope.()->Unit) = Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Outlined.ArrowBack,"뒤로",Modifier.clickable { back() },tint=Green); Text(title,Modifier.padding(start=12.dp),color=Green,fontWeight=FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); content() }
@Composable internal fun PageWithBottomAction(title:String,back:()->Unit,action:@Composable ()->Unit,content:@Composable ColumnScope.()->Unit) = Column(Modifier.fillMaxSize().padding(20.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Outlined.ArrowBack,"뒤로",Modifier.clickable { back() },tint=Green); Text(title,Modifier.padding(start=12.dp),color=Green,fontWeight=FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(13.dp),content=content); Spacer(Modifier.height(12.dp)); action() }
@OptIn(ExperimentalMaterial3Api::class) @Composable internal fun Field(label:String,value:String,onChange:(String)->Unit,password:Boolean) = OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),label={Text(label)},leadingIcon={Icon(if(password) Icons.Outlined.Key else Icons.Outlined.Email,null)},visualTransformation=if(password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,singleLine=true,shape=RoundedCornerShape(12.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Green,focusedLabelColor=Green,unfocusedBorderColor=Border))
@Composable internal fun MainButton(label:String,color:Color,text:Color=Color.White,enabled:Boolean=true,bordered:Boolean=false,click:()->Unit) = Button(click,Modifier.fillMaxWidth().height(50.dp),enabled=enabled,shape=RoundedCornerShape(10.dp),colors=ButtonDefaults.buttonColors(containerColor=color,contentColor=text),border=if(bordered) androidx.compose.foundation.BorderStroke(1.dp,Border) else null){Text(label,fontWeight=FontWeight.Bold,fontSize=12.sp)}
@Composable internal fun Divider() = Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f).height(1.dp).background(Border));Text("또는",Modifier.padding(horizontal=10.dp),color=Secondary,fontSize=10.sp);Box(Modifier.weight(1f).height(1.dp).background(Border))}
@Composable internal fun Tip(text:String)=Card(colors=CardDefaults.cardColors(containerColor=PaleGreen),shape=RoundedCornerShape(12.dp)){Text(text,Modifier.padding(12.dp),color=Secondary,fontSize=11.sp)}
@Composable internal fun Notice(number:String,text:String,color:Color)=Card(colors=CardDefaults.cardColors(containerColor=color),shape=RoundedCornerShape(10.dp)){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text(number,color=Orange,fontWeight=FontWeight.Bold);Text(text,Modifier.padding(start=10.dp),fontSize=12.sp,color=DeepGreen)}}
@Composable internal fun CheckRow(text:String,checked:Boolean,click:()->Unit)=Card(Modifier.fillMaxWidth().clickable{click()},colors=CardDefaults.cardColors(containerColor=if(checked)PaleGreen else Color.White),shape=RoundedCornerShape(10.dp)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Text(if(checked)"●" else "○",color=Green);Text(text,Modifier.padding(start=10.dp),fontSize=12.sp,color=DeepGreen);Spacer(Modifier.weight(1f));Text("필수",color=Orange,fontSize=10.sp)}}
@Composable internal fun Permission(title:String,detail:String,badge:String)=Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(12.dp)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Spa,null,tint=Green);Column(Modifier.padding(start=10.dp).weight(1f)){Text(title,fontSize=12.sp,fontWeight=FontWeight.Bold,color=DeepGreen);Text(detail,fontSize=10.sp,color=Secondary)};Text(badge,color=if(badge=="필수")Orange else Green,fontSize=10.sp,fontWeight=FontWeight.Bold)}}
